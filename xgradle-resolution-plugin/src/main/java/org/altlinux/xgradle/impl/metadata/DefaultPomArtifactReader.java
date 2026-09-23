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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
        Set<Path> described = skip.stream().map(DefaultPomArtifactReader::realPath).collect(Collectors.toSet());
        List<XmvnArtifact> artifacts = pomFiles(pomsRoot).entrySet().stream()
                .filter(pom -> !described.contains(pom.getKey()))
                .flatMap(pom -> artifacts(pom.getValue(), pomsRoot.relativize(pom.getValue()), javaRoot))
                .collect(Collectors.toList());
        logger.info("Read {} artifacts from POMs without XMvn metadata in {}", artifacts.size(), pomsRoot);
        return artifacts;
    }

    /**
     * POM files by their real path. A POM reachable under several names, such as a
     * compatibility symlink, is read once, preferably under its own name.
     */
    private static Map<Path, Path> pomFiles(Path root) {
        try (Stream<Path> files = Files.walk(root)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".pom"))
                    .sorted(Comparator.comparing(Files::isSymbolicLink).thenComparing(Comparator.naturalOrder()))
                    .collect(Collectors.toMap(
                            DefaultPomArtifactReader::realPath, Function.identity(), (first, second) -> first, TreeMap::new));
        } catch (IOException e) {
            throw new GradleException("Cannot list POM directory " + root, e);
        }
    }

    private static Path realPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }

    /**
     * Where the jar of a POM can be installed, relative to the jar root: under the
     * POM's own path, as xgradle-cli installs it ({@code X/Y.pom} and {@code X/Y.jar}),
     * under the JPP names of older packages ({@code JPP-Y.pom} for {@code Y.jar},
     * {@code JPP.X-Y.pom} for {@code X/Y.jar}), or named after the artifactId.
     */
    private static Stream<Path> jarCandidates(Path relativePom, String artifactId) {
        String name = relativePom.getFileName().toString();
        String base = name.substring(0, name.length() - ".pom".length());
        return Stream.of(Stream.of(base), jppNames(base), Stream.of(artifactId))
                .flatMap(Function.identity())
                .map(jar -> relativePom.resolveSibling(jar + ".jar"));
    }

    /** The directory of {@code JPP.X-Y} may contain dashes itself, so every split is a candidate. */
    private static Stream<String> jppNames(String base) {
        if (base.startsWith("JPP-")) {
            return Stream.of(base.substring("JPP-".length()));
        }
        if (!base.startsWith("JPP.")) {
            return Stream.empty();
        }
        String name = base.substring("JPP.".length());
        return IntStream.range(1, name.length() - 1)
                .filter(i -> name.charAt(i) == '-')
                .mapToObj(i -> name.substring(0, i) + "/" + name.substring(i + 1));
    }

    private Stream<XmvnArtifact> artifacts(Path pom, Path relativePom, Path javaRoot) {
        MavenCoordinate coordinate = pomParser.parsePom(pom);
        if (coordinate == null || !coordinate.isValid()) {
            logger.warn("Skipping POM without complete coordinates: {}", pom);
            return Stream.empty();
        }
        List<XmvnDependency> dependencies = pomParser.parseDependencies(pom).stream()
                .filter(dependency -> RUNTIME_SCOPES.contains(dependency.getScope()))
                .map(DefaultPomArtifactReader::toDependency)
                .collect(Collectors.toList());

        // The packaging says nothing here: ALT POMs of jar modules often declare <packaging>pom</packaging>.
        Stream<XmvnArtifact> jarArtifact = jarCandidates(relativePom, coordinate.getArtifactId())
                .map(javaRoot::resolve)
                .filter(Files::isRegularFile)
                .findFirst()
                .map(jar -> artifact(coordinate, ArtifactKey.DEFAULT_EXTENSION, jar, dependencies, pom))
                .stream();
        XmvnArtifact pomArtifact = artifact(coordinate, ArtifactKey.POM_EXTENSION, pom, dependencies, pom);
        return Stream.concat(jarArtifact, Stream.of(pomArtifact));
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
