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


import org.altlinux.xgradle.impl.model.IvyRepository;
import org.altlinux.xgradle.interfaces.metadata.IvyRepositoryGenerator;
import org.altlinux.xgradle.interfaces.metadata.SystemRepository;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.testfixtures.ProjectBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    private IvyRepositoryGenerator generator;

    private void load(List<Path> metadata) {
        load(metadata, true);
    }

    private void load(List<Path> metadata, boolean ignoreDuplicates) {
        generator = Installations.injector(Installations.metadataOnly(metadata, ignoreDuplicates), mock(Logger.class))
                .getInstance(IvyRepositoryGenerator.class);
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

        load(List.of(metadata));
        IvyRepository repository = generator.generate(temp.resolve("cache"));

        assertEquals(Set.of("app.jar", "lib.jar"), resolve(repository, "org.example:app:2.0"),
                "the optional dependency is left out and the missing one is skipped");
        assertEquals(Set.of("lib.jar"), resolve(repository, "org.example:lib-legacy:1.5"),
                "an alias resolves to the aliased module");
        assertEquals(Set.of("lib.jar"), resolve(repository, "org.example:lib:1.5", "org.renamed:lib:1.5"),
                "old and new coordinates of a relocated artifact put one jar on the classpath");
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
        load(List.of(metadata));
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
        load(List.of(Path.of(Objects.requireNonNull(getClass().getResource("/xmvn-metadata")).toURI())));

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
    @DisplayName("publishes the jar of a module that also installs other files")
    void publishesJarOverOtherExtensions(@TempDir Path metadata) throws IOException {
        Files.writeString(metadata.resolve("a.xml"), metadataFile(
                "<artifact><groupId>g</groupId><artifactId>a</artifactId><version>1</version>"
                        + "<path>" + temp.resolve("a.jar") + "</path></artifact>"
                        + "<artifact><groupId>g</groupId><artifactId>a</artifactId><extension>zip</extension>"
                        + "<version>1</version><path>" + temp.resolve("a.zip") + "</path></artifact>"));
        load(List.of(metadata));

        Path root = generator.generate(temp.resolve("cache")).getRoot();
        String descriptor = Files.readString(root.resolve("g/a/1/ivy.xml"));

        assertTrue(descriptor.contains("<artifact name=\"a\" type=\"jar\" ext=\"jar\""), descriptor);
    }

    @Test
    @DisplayName("resolves a dependency by its requested version, like XMvn")
    void dependencyUsesRequestedVersion(@TempDir Path metadata) throws IOException {
        Files.writeString(metadata.resolve("lib.xml"), metadataFile(
                artifact("g", "lib", "2.0", temp.resolve("lib.jar"), "")
                        + artifact("g", "lib", "1.0", temp.resolve("lib1.jar"), "",
                        "<compatVersions><version>1.0</version></compatVersions>")));
        Files.writeString(metadata.resolve("app.xml"), metadataFile(
                artifact("g", "app", "1", temp.resolve("app.jar"),
                        dependency("g", "lib", "1.0", false) + dependency("g", "other-lib", "9", false))
                        + artifact("g", "other-lib", "3", temp.resolve("other.jar"), "")));
        load(List.of(metadata));

        Path root = generator.generate(temp.resolve("cache")).getRoot();
        String descriptor = Files.readString(root.resolve("g/app/1/ivy.xml"));

        assertTrue(descriptor.contains("name=\"lib\" rev=\"1.0\""), "the compat version is picked: " + descriptor);
        assertTrue(descriptor.contains("name=\"other-lib\" rev=\"3\""), "no compat version falls back: " + descriptor);
    }

    @Test
    @DisplayName("an alias of a classified artifact depends on that artifact, not the main jar")
    void aliasKeepsClassifier(@TempDir Path metadata) throws IOException {
        Files.writeString(metadata.resolve("a.xml"), metadataFile(
                "<artifact><groupId>g</groupId><artifactId>a</artifactId><classifier>tests</classifier>"
                        + "<version>1</version><path>" + temp.resolve("a-tests.jar") + "</path>"
                        + "<aliases><alias><groupId>old</groupId><artifactId>a-tests</artifactId></alias></aliases>"
                        + "</artifact>"));
        load(List.of(metadata));

        String descriptor = Files.readString(generator.generate(temp.resolve("cache")).getRoot()
                .resolve("old/a-tests/1/ivy.xml"));

        assertTrue(descriptor.contains("m:classifier=\"tests\""), descriptor);
    }

    @Test
    @DisplayName("reuses the repository for unchanged metadata")
    void reusesRepository(@TempDir Path metadata) throws IOException {
        Files.writeString(metadata.resolve("a.xml"), metadataFile(artifact("g", "a", "1", temp.resolve("a.jar"), "")));
        load(List.of(metadata));

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

        load(List.of(metadata), true);
        Path ignoring = generator.generate(temp.resolve("cache")).getRoot();
        load(List.of(metadata), false);
        Path keeping = generator.generate(temp.resolve("cache")).getRoot();

        assertNotEquals(ignoring, keeping);
        assertFalse(Files.exists(ignoring.resolve("g/a/1/ivy.xml")), "both claims are dropped");
        assertEquals(temp.resolve("b.jar"), Files.readSymbolicLink(keeping.resolve("g/a/1/a-1.jar")));
    }

    @Test
    @DisplayName("removes repositories unused for a week and keeps recent ones")
    void removesUnusedRepositories(@TempDir Path metadata) throws IOException {
        Path cache = Files.createDirectories(temp.resolve("cache"));
        Path stale = Files.createDirectories(cache.resolve("stale"));
        Path recent = Files.createDirectories(cache.resolve("recent"));
        Path deadTmp = Files.createDirectories(cache.resolve("stale.12345"));
        Files.createFile(stale.resolve(".complete"));
        Files.createFile(recent.resolve(".complete"));
        FileTime weekAgo = FileTime.from(Instant.now().minus(Duration.ofDays(8)));
        Files.setLastModifiedTime(stale.resolve(".complete"), weekAgo);
        Files.setLastModifiedTime(deadTmp, weekAgo);

        Files.writeString(metadata.resolve("a.xml"), metadataFile(artifact("g", "a", "1", temp.resolve("a.jar"), "")));
        load(List.of(metadata));
        Path current = generator.generate(cache).getRoot();

        try (Stream<Path> entries = Files.list(cache)) {
            assertEquals(Set.of(current, recent), entries.collect(Collectors.toSet()));
        }
    }

    @Test
    @DisplayName("replaces a leftover repository directory without the complete marker")
    void replacesIncompleteRepository(@TempDir Path metadata) throws IOException {
        Files.writeString(metadata.resolve("a.xml"), metadataFile(artifact("g", "a", "1", temp.resolve("a.jar"), "")));
        load(List.of(metadata));
        Path root = generator.generate(temp.resolve("cache")).getRoot();
        Files.delete(root.resolve(".complete"));
        Files.writeString(root.resolve("junk"), "left by a crashed build");

        load(List.of(metadata));
        Path regenerated = generator.generate(temp.resolve("cache")).getRoot();

        assertEquals(root, regenerated);
        assertTrue(Files.isRegularFile(regenerated.resolve(".complete")));
        assertFalse(Files.exists(regenerated.resolve("junk")));
        try (Stream<Path> entries = Files.list(temp.resolve("cache"))) {
            assertEquals(List.of(root), entries.collect(Collectors.toList()), "no temporary directory is left");
        }
    }

    @Test
    @DisplayName("gives no system repository when nothing is installed, and warns once")
    void noRepositoryWithoutArtifacts(@TempDir Path metadata) {
        Logger logger = mock(Logger.class);
        SystemRepository systemRepository = Installations
                .injector(Installations.metadataOnly(List.of(metadata), true), logger)
                .getInstance(SystemRepository.class);
        Gradle gradle = mock(Gradle.class);

        assertTrue(systemRepository.forBuild(gradle).isEmpty());
        assertTrue(systemRepository.forBuild(gradle).isEmpty());
        verify(logger, times(1)).warn(startsWith("No installed artifacts found"));
    }

    @Test
    @DisplayName("leaves the repository readable to other users")
    void repositoryIsReadableByAll(@TempDir Path metadata) throws IOException {
        Files.writeString(metadata.resolve("a.xml"), metadataFile(artifact("g", "a", "1", temp.resolve("a.jar"), "")));
        load(List.of(metadata));

        Path root = generator.generate(temp.resolve("cache")).getRoot();

        assumeTrue(Files.getFileAttributeView(root, PosixFileAttributeView.class) != null);
        assertEquals("rwxr-xr-x",
                PosixFilePermissions.toString(Files.getPosixFilePermissions(root)));
    }

    private Set<String> resolve(IvyRepository repository, String... notations) {
        Project project = ProjectBuilder.builder().withProjectDir(temp.resolve("project").toFile()).build();
        addRepository(project, repository);
        Configuration configuration = project.getConfigurations().detachedConfiguration(
                Arrays.stream(notations)
                        .map(project.getDependencies()::create)
                        .toArray(Dependency[]::new));
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
