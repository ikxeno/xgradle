/*
 * Copyright 2025 BaseALT Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.altlinux.xgradle.impl.collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.impl.models.SbomLicense;
import org.altlinux.xgradle.interfaces.collectors.SbomComponentCollector;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.services.PomMetadata;
import org.altlinux.xgradle.interfaces.services.PomMetadataLicense;
import org.altlinux.xgradle.interfaces.services.PomMetadataReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Collects SBOM components from resolved Maven coordinates and resolved JAR files.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
public final class DefaultSbomComponentCollector implements SbomComponentCollector {

    private final PomMetadataReader pomMetadataReader;
    private final MetadataIndex index;
    private final PomFinder pomFinder;

    @Inject
    public DefaultSbomComponentCollector(PomMetadataReader pomMetadataReader, MetadataIndex index, PomFinder pomFinder) {
        this.pomMetadataReader = pomMetadataReader;
        this.index = index;
        this.pomFinder = pomFinder;
    }

    @Override
    public List<SbomComponent> collect(
            Collection<MavenCoordinate> artifacts,
            Collection<MavenCoordinate> pluginArtifacts,
            Collection<File> resolvedJars
    ) {
        LinkedHashMap<String, SbomComponent> components = new LinkedHashMap<>();
        Map<Path, PomMetadata> metadataByPomPath = new LinkedHashMap<>();

        appendLibraryComponents(artifacts, components, metadataByPomPath);
        appendPluginComponents(pluginArtifacts, components, metadataByPomPath);
        appendResolvedJarComponents(resolvedJars, components, metadataByPomPath);

        return new ArrayList<>(components.values());
    }

    private void appendLibraryComponents(
            Collection<MavenCoordinate> artifacts,
            LinkedHashMap<String, SbomComponent> components,
            Map<Path, PomMetadata> metadataByPomPath
    ) {
        if (artifacts == null) {
            return;
        }

        artifacts.stream()
                .filter(this::isEligibleCoordinate)
                .filter(coordinate -> !components.containsKey(uniqueKey(coordinate)))
                .forEach(coordinate -> {
                    PomMetadata metadata = readPomMetadata(coordinate, metadataByPomPath);
                    SbomComponent component = SbomComponent.maven(
                            coordinate.getGroupId(),
                            coordinate.getArtifactId(),
                            coordinate.getVersion(),
                            metadata.getProjectUrl(),
                            metadata.getScmUrl(),
                            toSbomLicenses(metadata.getLicenses())
                    );
                    components.put(uniqueKey(coordinate), component);
                });
    }

    private void appendPluginComponents(
            Collection<MavenCoordinate> artifacts,
            LinkedHashMap<String, SbomComponent> components,
            Map<Path, PomMetadata> metadataByPomPath
    ) {
        if (artifacts == null) {
            return;
        }

        artifacts.stream()
                .filter(this::isEligibleCoordinate)
                .forEach(coordinate -> {
                    PomMetadata metadata = readPomMetadata(coordinate, metadataByPomPath);
                    SbomComponent component = SbomComponent.mavenPlugin(
                            coordinate.getGroupId(),
                            coordinate.getArtifactId(),
                            coordinate.getVersion(),
                            metadata.getProjectUrl(),
                            metadata.getScmUrl(),
                            toSbomLicenses(metadata.getLicenses())
                    );
                    components.put(uniqueKey(coordinate), component);
                });
    }

    /**
     * Adds every resolved jar, including transitive ones. A jar of an installed artifact
     * gets its coordinates and POM metadata; any other jar is listed by file name.
     */
    private void appendResolvedJarComponents(
            Collection<File> resolvedJars,
            Map<String, SbomComponent> components,
            Map<Path, PomMetadata> metadataByPomPath
    ) {
        if (resolvedJars == null) {
            return;
        }

        Map<Path, XmvnArtifact> installed = index.artifacts().stream()
                .filter(artifact -> artifact.getPath() != null)
                .collect(Collectors.toMap(
                        artifact -> realPath(artifact.getPath()), Function.identity(), (first, second) -> first));
        resolvedJars.stream()
                .filter(jar -> jar != null && jar.isFile())
                .sorted()
                .map(jar -> Optional.ofNullable(installed.get(realPath(jar.toPath())))
                        .map(artifact -> installedComponent(artifact, metadataByPomPath))
                        .orElseGet(() -> SbomComponent.file(jar.getName())))
                .forEach(component -> components.putIfAbsent(component.uniqueKey(), component));
    }

    private SbomComponent installedComponent(XmvnArtifact artifact, Map<Path, PomMetadata> metadataByPomPath) {
        MavenCoordinate coordinate = MavenCoordinate.builder()
                .groupId(artifact.getGroupId())
                .artifactId(artifact.getArtifactId())
                .version(artifact.getVersion())
                .pomPath(Optional.ofNullable(pomFinder.findPomForArtifact(artifact.getGroupId(), artifact.getArtifactId()))
                        .map(MavenCoordinate::getPomPath)
                        .orElse(null))
                .build();
        PomMetadata metadata = readPomMetadata(coordinate, metadataByPomPath);
        return SbomComponent.maven(
                coordinate.getGroupId(),
                coordinate.getArtifactId(),
                coordinate.getVersion(),
                metadata.getProjectUrl(),
                metadata.getScmUrl(),
                toSbomLicenses(metadata.getLicenses())
        );
    }

    private static Path realPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }

    private boolean isEligibleCoordinate(MavenCoordinate coordinate) {
        return coordinate != null
                && !coordinate.isPomOnly()
                && coordinate.getGroupId() != null
                && coordinate.getArtifactId() != null;
    }

    private String uniqueKey(MavenCoordinate coordinate) {
        String version = coordinate.getVersion();
        return coordinate.getGroupId()
                + ":"
                + coordinate.getArtifactId()
                + ":"
                + (version != null ? version : "");
    }

    private PomMetadata readPomMetadata(
            MavenCoordinate coordinate,
            Map<Path, PomMetadata> metadataByPomPath
    ) {
        Path pomPath = coordinate.getPomPath();
        if (pomPath == null) {
            return PomMetadata.empty();
        }

        PomMetadata cached = metadataByPomPath.get(pomPath);
        if (cached != null) {
            return cached;
        }

        PomMetadata metadata = pomMetadataReader.read(pomPath);
        PomMetadata normalized = metadata != null ? metadata : PomMetadata.empty();
        metadataByPomPath.put(pomPath, normalized);
        return normalized;
    }

    private List<SbomLicense> toSbomLicenses(List<PomMetadataLicense> metadataLicenses) {
        if (metadataLicenses == null || metadataLicenses.isEmpty()) {
            return List.of();
        }

        return metadataLicenses.stream()
                .filter(metadataLicense -> metadataLicense != null)
                .map(metadataLicense -> new SbomLicense(metadataLicense.getName(), metadataLicense.getUrl()))
                .collect(Collectors.toUnmodifiableList());
    }
}
