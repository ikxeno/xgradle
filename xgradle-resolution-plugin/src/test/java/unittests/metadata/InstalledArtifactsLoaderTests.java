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

import org.altlinux.xgradle.impl.caches.CachesModule;
import org.altlinux.xgradle.impl.maven.MavenModule;
import org.altlinux.xgradle.impl.metadata.MetadataModule;
import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.IvyRepository;
import org.altlinux.xgradle.impl.parsers.ParsersModule;
import org.altlinux.xgradle.interfaces.metadata.InstalledArtifactsLoader;
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
@DisplayName("Installed artifacts loader")
class InstalledArtifactsLoaderTests {

    private static final String PACKAGE = "biz-aQute-bnd-gradle-plugins";

    @TempDir
    Path temp;

    private Injector injector;
    private Path poms;
    private Path java;

    @BeforeEach
    void setUp() throws IOException, URISyntaxException {
        injector = Guice.createInjector(
                new MetadataModule(), new ParsersModule(), new CachesModule(), new MavenModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Logger.class).toInstance(mock(Logger.class));
                    }
                });
        poms = Path.of(Objects.requireNonNull(getClass().getResource("/installed-poms")).toURI());
        java = Files.createDirectories(temp.resolve("java").resolve(PACKAGE));
        Files.writeString(java.resolve("biz.aQute.bnd.gradle.jar"), "bnd");
    }

    @Test
    @DisplayName("resolves a plugin marker through the POMs to the implementation jar")
    void resolvesMarkerChain() {
        injector.getInstance(InstalledArtifactsLoader.class).load(List.of(), true, poms, temp.resolve("java"));
        IvyRepository repository = injector.getInstance(IvyRepositoryGenerator.class).generate(temp.resolve("cache"));

        assertEquals(Set.of("biz.aQute.bnd.gradle.jar"),
                resolve(repository, "biz.aQute.bnd.builder:biz.aQute.bnd.builder.gradle.plugin:7.1.0"));
        assertEquals(Set.of("biz.aQute.bnd.gradle.jar"),
                resolve(repository, "biz.aQute.bnd.workspace:biz.aQute.bnd.workspace.gradle.plugin:7.1.0"));
    }

    @Test
    @DisplayName("lets XMvn metadata win over a POM for the same module")
    void metadataWins() throws IOException {
        Path metadata = Files.createDirectories(temp.resolve("metadata"));
        Files.writeString(metadata.resolve("bnd.xml"), "<metadata><artifacts><artifact>"
                + "<groupId>biz.aQute.bnd</groupId><artifactId>biz.aQute.bnd.gradle</artifactId>"
                + "<version>9.9</version><path>/usr/share/java/bnd-from-metadata.jar</path>"
                + "</artifact></artifacts></metadata>");

        injector.getInstance(InstalledArtifactsLoader.class).load(List.of(metadata), true, poms, temp.resolve("java"));
        MetadataIndex index = injector.getInstance(MetadataIndex.class);

        assertEquals("9.9", index.resolve(ArtifactKey.jar("biz.aQute.bnd", "biz.aQute.bnd.gradle", "SYSTEM"))
                .orElseThrow().getVersion());
        assertTrue(index.resolve(ArtifactKey.jar("biz.aQute.bnd.builder", "biz.aQute.bnd.builder.gradle.plugin", "SYSTEM"))
                .isEmpty(), "a marker is POM-only and has no jar");
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
                .map(InstalledArtifactsLoaderTests::realName)
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
