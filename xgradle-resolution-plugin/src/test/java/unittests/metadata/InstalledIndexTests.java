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

import org.altlinux.xgradle.impl.maven.MavenModule;
import org.altlinux.xgradle.impl.metadata.MetadataModule;
import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.InstalledLayout;
import org.altlinux.xgradle.impl.model.IvyRepository;
import org.altlinux.xgradle.impl.parsers.ParsersModule;
import org.altlinux.xgradle.interfaces.metadata.IvyRepositoryGenerator;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;

import org.gradle.api.Project;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Loads artifacts installed without XMvn metadata, using the POMs of ALT's
 * biz-aQute-bnd-gradle-plugins package, whose plugin markers point at the
 * implementation module.
 *
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("Installed artifacts index")
class InstalledIndexTests {

    private static final String PACKAGE = "biz-aQute-bnd-gradle-plugins";

    @TempDir
    Path temp;

    private Path poms;
    private Path java;

    @BeforeEach
    void setUp() throws IOException, URISyntaxException {
        poms = Path.of(Objects.requireNonNull(getClass().getResource("/installed-poms")).toURI());
        java = Files.createDirectories(temp.resolve("java").resolve(PACKAGE));
        Files.writeString(java.resolve("biz.aQute.bnd.gradle.jar"), "bnd");
    }

    @Test
    @DisplayName("resolves a plugin marker through the POMs to the implementation jar")
    void resolvesMarkerChain() {
        Injector injector = install(List.of(), true, poms, temp.resolve("java"));
        IvyRepository repository = injector.getInstance(IvyRepositoryGenerator.class).generate(temp.resolve("cache"));

        assertEquals(Set.of("biz.aQute.bnd.gradle.jar"),
                resolve(repository, "biz.aQute.bnd.builder:biz.aQute.bnd.builder.gradle.plugin:7.1.0"));
        assertEquals(Set.of("biz.aQute.bnd.gradle.jar"),
                resolve(repository, "biz.aQute.bnd.workspace:biz.aQute.bnd.workspace.gradle.plugin:7.1.0"));
    }

    @Test
    @DisplayName("reads a POM reachable through a symlink once")
    void readsSymlinkedPomOnce() throws IOException {
        Path copy = Files.createDirectories(temp.resolve("poms").resolve(PACKAGE));
        try (Stream<Path> files = Files.list(poms.resolve(PACKAGE))) {
            for (Path pom : files.collect(Collectors.toList())) {
                Files.copy(pom, copy.resolve(pom.getFileName()));
            }
        }
        Files.createSymbolicLink(copy.getParent().resolve("JPP-biz.aQute.bnd.gradle.pom"),
                copy.resolve("biz.aQute.bnd.gradle.pom"));

        Injector injector = install(List.of(), true, copy.getParent(), temp.resolve("java"));

        assertTrue(injector.getInstance(MetadataIndex.class)
                .resolve(new ArtifactKey("biz.aQute.bnd", "biz.aQute.bnd.gradle", "pom", "", "SYSTEM"))
                .isPresent(), "the POM is not dropped as its own duplicate");
    }

    @Test
    @DisplayName("finds the jar of a POM installed under a JPP name")
    void findsJarOfJppPom() throws IOException {
        Path jppPoms = Files.createDirectories(temp.resolve("jpp-poms"));
        Files.copy(poms.resolve(PACKAGE).resolve("biz.aQute.bnd.gradle.pom"),
                jppPoms.resolve("JPP." + PACKAGE + "-biz.aQute.bnd.gradle.pom"));

        Injector injector = install(List.of(), true, jppPoms, temp.resolve("java"));

        assertEquals(java.resolve("biz.aQute.bnd.gradle.jar"), injector.getInstance(MetadataIndex.class)
                .resolve(ArtifactKey.jar("biz.aQute.bnd", "biz.aQute.bnd.gradle", "SYSTEM"))
                .orElseThrow().getPath());
    }

    @Test
    @DisplayName("lets XMvn metadata win over a POM for the same module")
    void metadataWins() throws IOException {
        Path metadata = Files.createDirectories(temp.resolve("metadata"));
        Files.writeString(metadata.resolve("bnd.xml"), "<metadata><artifacts><artifact>"
                + "<groupId>biz.aQute.bnd</groupId><artifactId>biz.aQute.bnd.gradle</artifactId>"
                + "<version>9.9</version><path>/usr/share/java/bnd-from-metadata.jar</path>"
                + "</artifact></artifacts></metadata>");

        Injector injector = install(List.of(metadata), true, poms, temp.resolve("java"));
        MetadataIndex index = injector.getInstance(MetadataIndex.class);

        assertEquals("9.9", index.resolve(ArtifactKey.jar("biz.aQute.bnd", "biz.aQute.bnd.gradle", "SYSTEM"))
                .orElseThrow().getVersion());
        ArtifactKey marker = ArtifactKey.jar("biz.aQute.bnd.builder", "biz.aQute.bnd.builder.gradle.plugin", "SYSTEM");
        assertTrue(index.resolve(marker).isEmpty(), "a marker is POM-only and has no jar");
    }

    private static Injector install(List<Path> metadata, boolean ignoreDuplicates, Path pomsRoot, Path javaRoot) {
        return Guice.createInjector(
                new MetadataModule(new InstalledLayout(metadata, ignoreDuplicates, pomsRoot, javaRoot)),
                new ParsersModule(), new MavenModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Logger.class).toInstance(mock(Logger.class));
                    }
                });
    }

    private Set<String> resolve(IvyRepository repository, String notation) {
        Project project = ProjectBuilder.builder().withProjectDir(temp.resolve("project").toFile()).build();
        project.getRepositories().ivy(repo -> {
            repo.setUrl(repository.getRoot().toUri());
            repo.patternLayout(layout -> {
                layout.ivy(IvyRepositoryGenerator.IVY_PATTERN);
                layout.artifact(IvyRepositoryGenerator.ARTIFACT_PATTERN);
            });
            repo.metadataSources(sources -> sources.ivyDescriptor());
        });
        return project.getConfigurations().detachedConfiguration(project.getDependencies().create(notation))
                .resolve().stream()
                .map(File::toPath)
                .map(InstalledIndexTests::realName)
                .collect(Collectors.toSet());
    }

    private static String realName(Path file) {
        try {
            return file.toRealPath().getFileName().toString();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
