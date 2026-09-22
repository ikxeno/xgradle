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
package unittests.metadata;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.altlinux.xgradle.impl.metadata.MetadataModule;
import org.altlinux.xgradle.impl.model.IvyRepository;
import org.altlinux.xgradle.interfaces.metadata.IvyRepositoryGenerator;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.parsers.PomParser;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.testfixtures.ProjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests the ivy repository generated from XMvn metadata, including a real
 * Gradle resolution against it.
 *
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("Ivy repository generator")
class IvyRepositoryGeneratorTests {

    @TempDir
    Path temp;

    private MetadataIndex index;
    private IvyRepositoryGenerator generator;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(new MetadataModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(Logger.class).toInstance(mock(Logger.class));
                bind(PomParser.class).toInstance(mock(PomParser.class));
            }
        });
        index = injector.getInstance(MetadataIndex.class);
        generator = injector.getInstance(IvyRepositoryGenerator.class);
    }

    @Test
    @DisplayName("Gradle resolves transitive dependencies from the generated descriptors")
    void gradleResolvesTransitives() throws IOException {
        Path metadata = Files.createDirectories(temp.resolve("metadata"));
        Path jars = Files.createDirectories(temp.resolve("java"));
        Files.writeString(metadata.resolve("app.xml"), metadataFile(
                artifact("org.example", "app", "2.0", jars.resolve("app.jar"),
                        dependency("org.example", "lib", "1.0", false)
                                + dependency("org.example", "absent", "1.0", false)
                                + dependency("org.example", "extra", "1.0", true))));
        Files.writeString(metadata.resolve("lib.xml"), metadataFile(
                artifact("org.example", "lib", "1.5", jars.resolve("lib.jar"), "",
                        "<aliases><alias><groupId>org.example</groupId><artifactId>lib-legacy</artifactId></alias>"
                                + "<alias><groupId>org.renamed</groupId><artifactId>lib</artifactId></alias></aliases>")));
        Files.writeString(metadata.resolve("extra.xml"), metadataFile(
                artifact("org.example", "extra", "1.0", jars.resolve("extra.jar"), "")));
        Files.writeString(jars.resolve("app.jar"), "app");
        Files.writeString(jars.resolve("lib.jar"), "lib");
        Files.writeString(jars.resolve("extra.jar"), "extra");

        index.build(List.of(metadata));
        IvyRepository repository = generator.generate(temp.resolve("cache"));

        assertEquals(Set.of("app.jar", "lib.jar"), resolve(repository, "org.example:app:2.0"),
                "the optional dependency is left out and the missing one is skipped");
        assertEquals(Set.of("lib.jar"), resolve(repository, "org.example:lib-legacy:1.5"),
                "an alias resolves to the aliased module");
        assertEquals(Set.of("lib.jar"), resolve(repository, "org.example:lib:1.5", "org.renamed:lib:1.5"),
                "old and new coordinates of a relocated artifact put one jar on the classpath");
        assertEquals(List.of("org.example:app:2.0 -> org.example:absent:1.0"),
                repository.getMissingDependencies());
    }

    @Test
    @DisplayName("platform() and enforcedPlatform() on a POM-only module resolve to nothing")
    void platformOnPomOnlyModule() throws IOException {
        Path metadata = Files.createDirectories(temp.resolve("metadata"));
        Path jars = Files.createDirectories(temp.resolve("java"));
        Files.writeString(metadata.resolve("bom.xml"), metadataFile(
                "<artifact><groupId>org.example</groupId><artifactId>bom</artifactId><extension>pom</extension>"
                        + "<version>7</version><path>" + jars.resolve("bom.pom") + "</path></artifact>"
                        + artifact("org.example", "lib", "1.5", jars.resolve("lib.jar"), "")));
        Files.writeString(jars.resolve("lib.jar"), "lib");
        index.build(List.of(metadata));
        IvyRepository repository = generator.generate(temp.resolve("cache"));

        Project project = ProjectBuilder.builder().withProjectDir(temp.resolve("platform").toFile()).build();
        project.getPluginManager().apply("java-library");
        addRepository(project, repository);
        project.getDependencies().add("implementation",
                project.getDependencies().platform("org.example:bom:7"));
        project.getDependencies().add("implementation",
                project.getDependencies().enforcedPlatform("org.example:bom:7"));
        project.getDependencies().add("implementation", "org.example:lib:1.5");

        Set<String> files = project.getConfigurations().getByName("compileClasspath").resolve().stream()
                .map(file -> resolveLink(file).getName())
                .collect(Collectors.toSet());
        assertEquals(Set.of("lib.jar"), files);
    }

    @Test
    @DisplayName("writes descriptors for real ALT metadata")
    void writesDescriptorsForAltMetadata() throws IOException, URISyntaxException {
        index.build(List.of(Path.of(Objects.requireNonNull(getClass().getResource("/xmvn-metadata")).toURI())));

        Path root = generator.generate(temp.resolve("cache")).getRoot();

        String guava = Files.readString(root.resolve("com.google.guava/guava/33.5.0-jre/ivy.xml"));
        assertTrue(guava.contains("<dependency org=\"com.google.guava\" name=\"failureaccess\" rev=\"1.0.3\""));
        assertEquals(Path.of("/usr/share/java/guava/guava.jar"),
                Files.readSymbolicLink(root.resolve("com.google.guava/guava/33.5.0-jre/guava-33.5.0-jre.jar")));

        String parent = Files.readString(root.resolve("com.google.guava/guava-parent/33.5.0-jre/ivy.xml"));
        assertTrue(parent.contains("<publications/>"), "a POM-only module publishes nothing");

        assertTrue(Files.isRegularFile(root.resolve("org.apache.maven/maven-model/2.0.7/ivy.xml")),
                "a compat artifact gets a module for each compat version");
        assertFalse(Files.exists(root.resolve("org.apache.maven/maven-model/SYSTEM")));
    }

    @Test
    @DisplayName("reuses the repository for unchanged metadata")
    void reusesRepository(@TempDir Path metadata) throws IOException {
        Files.writeString(metadata.resolve("a.xml"), metadataFile(artifact("g", "a", "1", temp.resolve("a.jar"), "")));
        index.build(List.of(metadata));

        Path first = generator.generate(temp.resolve("cache")).getRoot();
        Files.writeString(first.resolve("marker"), "kept");
        Path second = generator.generate(temp.resolve("cache")).getRoot();

        assertEquals(first, second);
        assertTrue(Files.exists(second.resolve("marker")));
    }

    @Test
    @DisplayName("writes a new repository when only the duplicate handling changes")
    void fingerprintFollowsContent() throws IOException {
        Path metadata = Files.createDirectories(temp.resolve("metadata"));
        Files.writeString(metadata.resolve("a.xml"), metadataFile(artifact("g", "a", "1", temp.resolve("a.jar"), "")));
        Files.writeString(metadata.resolve("b.xml"), metadataFile(artifact("g", "a", "1", temp.resolve("b.jar"), "")));

        index.build(List.of(metadata), true);
        Path ignoring = generator.generate(temp.resolve("cache")).getRoot();
        setUp();
        index.build(List.of(metadata), false);
        Path keeping = generator.generate(temp.resolve("cache")).getRoot();

        assertNotEquals(ignoring, keeping);
        assertFalse(Files.exists(ignoring.resolve("g/a/1/ivy.xml")), "both claims are dropped");
        assertEquals(temp.resolve("b.jar"), Files.readSymbolicLink(keeping.resolve("g/a/1/a-1.jar")));
    }

    @Test
    @DisplayName("replaces a leftover repository directory without the complete marker")
    void replacesIncompleteRepository(@TempDir Path metadata) throws IOException {
        Files.writeString(metadata.resolve("a.xml"), metadataFile(artifact("g", "a", "1", temp.resolve("a.jar"), "")));
        index.build(List.of(metadata));
        Path root = generator.generate(temp.resolve("cache")).getRoot();
        Files.delete(root.resolve(".complete"));
        Files.writeString(root.resolve("junk"), "left by a crashed build");

        setUp();
        index.build(List.of(metadata));
        Path regenerated = generator.generate(temp.resolve("cache")).getRoot();

        assertEquals(root, regenerated);
        assertTrue(Files.isRegularFile(regenerated.resolve(".complete")));
        assertFalse(Files.exists(regenerated.resolve("junk")));
        try (java.util.stream.Stream<Path> entries = Files.list(temp.resolve("cache"))) {
            assertEquals(List.of(root), entries.collect(Collectors.toList()), "no temporary directory is left");
        }
    }

    private Set<String> resolve(IvyRepository repository, String... notations) {
        Project project = ProjectBuilder.builder().withProjectDir(temp.resolve("project").toFile()).build();
        addRepository(project, repository);
        Configuration configuration = project.getConfigurations().detachedConfiguration(
                java.util.Arrays.stream(notations)
                        .map(project.getDependencies()::create)
                        .toArray(org.gradle.api.artifacts.Dependency[]::new));
        return configuration.resolve().stream()
                .map(file -> resolveLink(file).getName())
                .collect(Collectors.toSet());
    }

    private static void addRepository(Project project, IvyRepository repository) {
        project.getRepositories().ivy(repo -> {
            repo.setUrl(repository.getRoot().toUri());
            repo.patternLayout(layout -> {
                layout.ivy(IvyRepositoryGenerator.IVY_PATTERN);
                layout.artifact(IvyRepositoryGenerator.ARTIFACT_PATTERN);
            });
            repo.metadataSources(sources -> sources.ivyDescriptor());
        });
    }

    private static File resolveLink(File file) {
        try {
            return file.toPath().toRealPath().toFile();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static String metadataFile(String artifacts) {
        return "<metadata xmlns=\"http://fedorahosted.org/xmvn/METADATA/3.2.0\"><artifacts>"
                + artifacts + "</artifacts></metadata>";
    }

    private static String artifact(String groupId, String artifactId, String version, Path path, String dependencies) {
        return artifact(groupId, artifactId, version, path, dependencies, "");
    }

    private static String artifact(String groupId, String artifactId, String version, Path path,
                                   String dependencies, String extra) {
        return "<artifact><groupId>" + groupId + "</groupId><artifactId>" + artifactId + "</artifactId>"
                + "<version>" + version + "</version><path>" + path + "</path>" + extra
                + (dependencies.isEmpty() ? "" : "<dependencies>" + dependencies + "</dependencies>")
                + "</artifact>";
    }

    private static String dependency(String groupId, String artifactId, String version, boolean optional) {
        return "<dependency><groupId>" + groupId + "</groupId><artifactId>" + artifactId + "</artifactId>"
                + "<requestedVersion>" + version + "</requestedVersion>"
                + (optional ? "<optional>true</optional>" : "") + "</dependency>";
    }
}
