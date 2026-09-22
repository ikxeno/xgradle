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
import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.impl.model.XmvnDependency;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests the XMvn metadata index against metadata taken from ALT Sisyphus packages.
 *
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("XMvn metadata index")
class MetadataIndexTests {

    private Logger logger;
    private MetadataIndex index;
    private Path fixtures;

    @BeforeEach
    void setUp() throws URISyntaxException {
        logger = mock(Logger.class);
        Injector injector = Guice.createInjector(new MetadataModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(Logger.class).toInstance(logger);
            }
        });
        index = injector.getInstance(MetadataIndex.class);
        fixtures = Path.of(Objects.requireNonNull(getClass().getResource("/xmvn-metadata")).toURI());
    }

    @Test
    @DisplayName("resolves any requested version of a plain artifact to the installed one")
    void resolvesSystemVersion() {
        index.build(List.of(fixtures));

        XmvnArtifact guava = index.resolve(ArtifactKey.jar("com.google.guava", "guava", "31.0-jre")).orElseThrow();

        assertEquals("33.5.0-jre", guava.getVersion());
        assertEquals(Path.of("/usr/share/java/guava/guava.jar"), guava.getPath());
        assertEquals(
                List.of("com.google.guava:failureaccess:1.0.3",
                        "org.jspecify:jspecify:1.0.0",
                        "com.google.errorprone:error_prone_annotations:2.41.0"),
                guava.getDependencies().stream().map(XmvnDependency::toString).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("keeps the POM of a module apart from its jar")
    void separatesPomFromJar() {
        index.build(List.of(fixtures));

        ArtifactKey pom = new ArtifactKey("com.google.guava", "guava-parent", "pom", "", "SYSTEM");

        assertEquals(Path.of("/usr/share/maven-poms/guava/guava-parent.pom"),
                index.resolve(pom).orElseThrow().getPath());
        assertTrue(index.resolve(ArtifactKey.jar("com.google.guava", "guava-parent", "SYSTEM")).isEmpty());
    }

    @Test
    @DisplayName("resolves compat artifacts only by their compat versions")
    void resolvesCompatVersions() {
        index.build(List.of(fixtures));

        XmvnArtifact model = index.resolve(ArtifactKey.jar("org.apache.maven", "maven-model", "2.0.7")).orElseThrow();

        assertTrue(model.isCompat());
        assertEquals("2.2.1", model.getVersion());
        assertTrue(index.resolve(ArtifactKey.jar("org.apache.maven", "maven-model", "3.9.0")).isEmpty(),
                "a compat artifact has no SYSTEM key, so other versions must not resolve");
    }

    @Test
    @DisplayName("resolves aliases to the aliased artifact")
    void resolvesAliases() {
        index.build(List.of(fixtures));

        XmvnArtifact viaAlias = index.resolve(ArtifactKey.jar("org.hamcrest", "hamcrest-core", "1.3")).orElseThrow();

        assertEquals("hamcrest", viaAlias.getArtifactId());
        assertEquals(Path.of("/usr/share/java/hamcrest/hamcrest.jar"), viaAlias.getPath());
    }

    @Test
    @DisplayName("reads optional dependencies and classified artifacts")
    void readsOptionalAndClassifier() {
        index.build(List.of(fixtures));

        XmvnDependency plexusXml = index.resolve(ArtifactKey.jar("org.codehaus.plexus", "plexus-utils", "SYSTEM"))
                .orElseThrow().getDependencies().get(0);
        ArtifactKey sources = new ArtifactKey("org.slf4j", "slf4j-api", "jar", "sources", "SYSTEM");

        assertTrue(plexusXml.isOptional());
        assertEquals(Path.of("/usr/share/java/slf4j/slf4j-api-sources.jar"),
                index.resolve(sources).orElseThrow().getPath());
    }

    @Test
    @DisplayName("reads gzip-compressed metadata")
    void readsGzip(@TempDir Path dir) throws IOException {
        Path gz = dir.resolve("guava.xml");
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(gz))) {
            out.write(Files.readAllBytes(fixtures.resolve("guava-guava.xml")));
        }

        index.build(List.of(dir));

        assertTrue(index.resolve(ArtifactKey.jar("com.google.guava", "guava", "SYSTEM")).isPresent());
    }

    @Test
    @DisplayName("drops a key two artifacts claim, as XMvn does by default")
    void dropsDuplicateByDefault(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("a.xml"), metadata("/usr/share/java/a.jar"));
        Files.writeString(dir.resolve("b.xml"), metadata("/usr/share/java/b.jar"));

        index.build(List.of(dir));

        assertTrue(index.resolve(ArtifactKey.jar("g", "a", "SYSTEM")).isEmpty());
        verify(logger).warn(anyString(), any(Object.class), any(Object.class), any(Object.class));
    }

    @Test
    @DisplayName("like XMvn, lets a third artifact take a key dropped as a duplicate")
    void thirdDuplicateTakesDroppedKey(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("a.xml"), metadata("/usr/share/java/a.jar"));
        Files.writeString(dir.resolve("b.xml"), metadata("/usr/share/java/b.jar"));
        Files.writeString(dir.resolve("c.xml"), metadata("/usr/share/java/c.jar"));

        index.build(List.of(dir));

        assertEquals(Path.of("/usr/share/java/c.jar"),
                index.resolve(ArtifactKey.jar("g", "a", "SYSTEM")).orElseThrow().getPath());
    }

    @Test
    @DisplayName("with ignoreDuplicateMetadata=false reads files in name order and the later one wins")
    void laterFileWinsDuplicate(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("b.xml"), metadata("/usr/share/java/b.jar"));
        Files.writeString(dir.resolve("a.xml"), metadata("/usr/share/java/a.jar"));

        index.build(List.of(dir), false);

        assertEquals(Path.of("/usr/share/java/b.jar"),
                index.resolve(ArtifactKey.jar("g", "a", "SYSTEM")).orElseThrow().getPath());
    }

    @Test
    @DisplayName("fails on a missing location instead of skipping it")
    void failsOnMissingLocation(@TempDir Path dir) {
        assertThrows(GradleException.class, () -> index.build(List.of(dir.resolve("missing"))));
    }

    @Test
    @DisplayName("fails on a broken file instead of skipping it")
    void failsOnBrokenFile(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("broken.xml"), "<metadata><artifacts><artifact>");

        GradleException e = assertThrows(GradleException.class, () -> index.build(List.of(dir)));

        assertTrue(e.getMessage().contains("broken.xml"));
        verify(logger, never()).info(anyString(), any(Object.class), any(Object.class));
    }

    private static String metadata(String path) {
        return "<metadata xmlns=\"http://fedorahosted.org/xmvn/METADATA/3.2.0\"><artifacts><artifact>"
                + "<groupId>g</groupId><artifactId>a</artifactId><version>1</version>"
                + "<path>" + path + "</path>"
                + "</artifact></artifacts></metadata>";
    }
}
