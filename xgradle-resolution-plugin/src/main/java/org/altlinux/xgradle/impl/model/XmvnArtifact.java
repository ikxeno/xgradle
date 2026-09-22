/*
 * Copyright 2026 BaseALT Ltd
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
package org.altlinux.xgradle.impl.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Installed artifact described by an XMvn metadata file
 * ({@code /usr/share/maven-metadata/*.xml}).
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class XmvnArtifact {

    private final String groupId;
    private final String artifactId;
    private final String extension;
    private final String classifier;
    private final String version;
    private final Path path;
    private final String namespace;
    private final Map<String, String> properties;
    private final List<String> compatVersions;
    private final List<ArtifactKey> aliases;
    private final List<XmvnDependency> dependencies;
    private final Path metadataFile;

    public XmvnArtifact(
            String groupId,
            String artifactId,
            String extension,
            String classifier,
            String version,
            Path path,
            String namespace,
            Map<String, String> properties,
            List<String> compatVersions,
            List<ArtifactKey> aliases,
            List<XmvnDependency> dependencies,
            Path metadataFile
    ) {
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        this.artifactId = Objects.requireNonNull(artifactId, "artifactId");
        this.extension = extension == null || extension.isEmpty() ? ArtifactKey.DEFAULT_EXTENSION : extension;
        this.classifier = classifier == null ? "" : classifier;
        this.version = Objects.requireNonNull(version, "version");
        this.path = path;
        this.namespace = namespace == null ? "" : namespace;
        this.properties = Map.copyOf(properties);
        this.compatVersions = List.copyOf(compatVersions);
        this.aliases = List.copyOf(aliases);
        this.dependencies = List.copyOf(dependencies);
        this.metadataFile = metadataFile;
    }

    /**
     * Keys this artifact is reachable under: its own coordinates and every alias,
     * each with every compat version, or with {@link ArtifactKey#SYSTEM_VERSION}
     * if it is not a compat artifact.
     */
    public List<ArtifactKey> lookupKeys() {
        List<String> versions = compatVersions.isEmpty()
                ? List.of(ArtifactKey.SYSTEM_VERSION)
                : compatVersions;
        ArtifactKey self = new ArtifactKey(groupId, artifactId, extension, classifier, ArtifactKey.SYSTEM_VERSION);
        return Stream.concat(Stream.of(self), aliases.stream())
                .flatMap(base -> versions.stream().map(base::withVersion))
                .collect(Collectors.toList());
    }

    public boolean isCompat() {
        return !compatVersions.isEmpty();
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getExtension() {
        return extension;
    }

    public String getClassifier() {
        return classifier;
    }

    /**
     * Upstream version; never a compat version and never {@code SYSTEM}.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Absolute path of the installed file; {@code null} if the metadata has none.
     */
    public Path getPath() {
        return path;
    }

    public String getNamespace() {
        return namespace;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public List<String> getCompatVersions() {
        return compatVersions;
    }

    /**
     * Alias coordinates; their version is always {@link ArtifactKey#SYSTEM_VERSION}.
     */
    public List<ArtifactKey> getAliases() {
        return aliases;
    }

    public List<XmvnDependency> getDependencies() {
        return dependencies;
    }

    public Path getMetadataFile() {
        return metadataFile;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(groupId).append(':').append(artifactId).append(':').append(extension);
        if (!classifier.isEmpty()) {
            sb.append(':').append(classifier);
        }
        return sb.append(':').append(version).toString();
    }
}
