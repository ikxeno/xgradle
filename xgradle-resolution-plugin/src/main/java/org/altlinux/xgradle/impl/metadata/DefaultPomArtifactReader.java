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
 * Builds artifact records from installed POM files in the form XMvn records a
 * package: a jar and a POM per module, each with the compile and runtime
 * dependencies of the POM.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class DefaultPomArtifactReader implements PomArtifactReader {

    private static final Map<String, String> TYPE_EXTENSIONS = Map.of(
            "test-jar", ArtifactKey.DEFAULT_EXTENSION,
            "maven-plugin", ArtifactKey.DEFAULT_EXTENSION,
            "ejb", ArtifactKey.DEFAULT_EXTENSION,
            "ejb-client", ArtifactKey.DEFAULT_EXTENSION,
            "java-source", ArtifactKey.DEFAULT_EXTENSION,
            "javadoc", ArtifactKey.DEFAULT_EXTENSION,
            "bundle", ArtifactKey.DEFAULT_EXTENSION);
    private static final Map<String, String> TYPE_CLASSIFIERS = Map.of(
            "test-jar", "tests",
            "ejb-client", "client",
            "java-source", "sources",
            "javadoc", "javadoc");
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
        Set<Path> described = skip.stream().map(RealPaths::of).collect(Collectors.toSet());
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
                            RealPaths::of, Function.identity(), (first, second) -> first, TreeMap::new));
        } catch (IOException e) {
            throw new GradleException("Cannot list POM directory " + root, e);
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

    /**
     * The POM of a module and its jar, if one is installed. The packaging is ignored
     * because ALT POMs of jar modules often declare {@code <packaging>pom</packaging>}.
     */
    private Stream<XmvnArtifact> artifacts(Path pom, Path relativePom, Path javaRoot) {
        try {
            return readArtifacts(pom, relativePom, javaRoot);
        } catch (GradleException e) {
            logger.warn("Skipping unreadable POM {}: {}", pom, e.getMessage());
            return Stream.empty();
        }
    }

    private Stream<XmvnArtifact> readArtifacts(Path pom, Path relativePom, Path javaRoot) {
        MavenCoordinate coordinate = pomParser.parsePom(pom);
        if (coordinate == null || !coordinate.isValid()) {
            logger.warn("Skipping POM without complete coordinates: {}", pom);
            return Stream.empty();
        }
        List<XmvnDependency> dependencies = pomParser.parseDependencies(pom).stream()
                .filter(dependency -> RUNTIME_SCOPES.contains(dependency.getScope()))
                .map(DefaultPomArtifactReader::toDependency)
                .collect(Collectors.toList());

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
        return XmvnArtifact.builder(coordinate.getGroupId(), coordinate.getArtifactId(), coordinate.getVersion(), pom)
                .extension(extension)
                .path(path)
                .dependencies(dependencies)
                .build();
    }

    /**
     * A POM dependency as XMvn records it. The Maven type becomes an extension and
     * a classifier the way Maven's standard artifact handlers map it, and a version
     * with an unresolved property counts as missing.
     */
    private static XmvnDependency toDependency(MavenCoordinate dependency) {
        String type = dependency.getPackaging() == null ? ArtifactKey.DEFAULT_EXTENSION : dependency.getPackaging();
        String version = dependency.getVersion();
        return XmvnDependency.builder(dependency.getGroupId(), dependency.getArtifactId())
                .extension(TYPE_EXTENSIONS.getOrDefault(type, type))
                .classifier(dependency.getClassifier().isEmpty()
                        ? TYPE_CLASSIFIERS.getOrDefault(type, "")
                        : dependency.getClassifier())
                .requestedVersion(version == null || version.contains("${") ? null : version)
                .optional(dependency.isOptional())
                .exclusions(dependency.getExclusions())
                .build();
    }
}
