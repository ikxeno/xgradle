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
package org.altlinux.xgradle.impl.metadata;

import com.google.inject.Inject;

import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.impl.model.XmvnDependency;
import org.altlinux.xgradle.interfaces.metadata.PomArtifactReader;
import org.altlinux.xgradle.interfaces.parsers.PomParser;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds artifact records from installed POM files the way XMvn records a
 * package: the jar and the POM of a module, both carrying the compile and
 * runtime dependencies of the POM.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class DefaultPomArtifactReader implements PomArtifactReader {

    private static final Set<MavenScope> RUNTIME_SCOPES = Set.of(MavenScope.COMPILE, MavenScope.RUNTIME);

    private final PomParser pomParser;
    private final Logger logger;

    @Inject
    DefaultPomArtifactReader(PomParser pomParser, Logger logger) {
        this.pomParser = pomParser;
        this.logger = logger;
    }

    @Override
    public List<XmvnArtifact> read(Path pomsRoot, Path javaRoot, Set<Path> skip) {
        if (!Files.isDirectory(pomsRoot)) {
            return List.of();
        }
        List<XmvnArtifact> artifacts = pomFiles(pomsRoot)
                .filter(pom -> !skip.contains(pom))
                .flatMap(pom -> artifacts(pom, javaRoot.resolve(jarPath(pomsRoot.relativize(pom)))))
                .collect(Collectors.toList());
        logger.info("Read {} artifacts from POMs without XMvn metadata in {}", artifacts.size(), pomsRoot);
        return artifacts;
    }

    private static Stream<Path> pomFiles(Path root) {
        try (Stream<Path> files = Files.walk(root)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".pom"))
                    .sorted()
                    .collect(Collectors.toList())
                    .stream();
        } catch (IOException e) {
            throw new GradleException("Cannot list POM directory " + root, e);
        }
    }

    private static Path jarPath(Path relativePom) {
        String name = relativePom.getFileName().toString();
        return relativePom.resolveSibling(name.substring(0, name.length() - ".pom".length()) + ".jar");
    }

    private Stream<XmvnArtifact> artifacts(Path pom, Path jar) {
        MavenCoordinate coordinate = pomParser.parsePom(pom);
        if (coordinate == null || !coordinate.isValid()) {
            logger.warn("Skipping POM without complete coordinates: {}", pom);
            return Stream.empty();
        }
        List<XmvnDependency> dependencies = pomParser.parseDependencies(pom).stream()
                .filter(dependency -> RUNTIME_SCOPES.contains(dependency.getScope()))
                .map(DefaultPomArtifactReader::toDependency)
                .collect(Collectors.toList());

        Stream<XmvnArtifact> pomArtifact = Stream.of(artifact(coordinate, "pom", pom, dependencies, pom));
        // The packaging says nothing here: ALT POMs of jar modules often declare <packaging>pom</packaging>.
        return Files.isRegularFile(jar)
                ? Stream.concat(Stream.of(artifact(coordinate, ArtifactKey.DEFAULT_EXTENSION, jar, dependencies, pom)), pomArtifact)
                : pomArtifact;
    }

    private static XmvnArtifact artifact(
            MavenCoordinate coordinate,
            String extension,
            Path path,
            List<XmvnDependency> dependencies,
            Path pom
    ) {
        return new XmvnArtifact(
                coordinate.getGroupId(),
                coordinate.getArtifactId(),
                extension,
                "",
                coordinate.getVersion(),
                path,
                "",
                Map.of(),
                List.of(),
                List.of(),
                dependencies,
                pom);
    }

    private static XmvnDependency toDependency(MavenCoordinate dependency) {
        return new XmvnDependency(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                null,
                "",
                dependency.getVersion(),
                null,
                "",
                dependency.isOptional(),
                List.of());
    }
}
