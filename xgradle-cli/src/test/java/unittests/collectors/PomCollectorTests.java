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
package unittests.collectors;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.google.inject.util.Modules;
import org.altlinux.xgradle.impl.enums.ProcessingType;
import org.altlinux.xgradle.interfaces.collectors.ArtifactCollector;
import org.altlinux.xgradle.interfaces.collectors.PomCollector;
import org.altlinux.xgradle.impl.collectors.CollectorsModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PomCollector contract")
class PomCollectorTests {

    @TempDir
    Path tempDir;

    private PomCollector collector;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new CollectorsModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(ArtifactCollector.class).toInstance(
                                        (
                                                String searchingDir,
                                                List<String> artifactNames,
                                                ProcessingType processingType
                                        ) -> new HashMap<String, Path>()
                                );
                            }
                        })
        );

        collector = injector.getInstance(PomCollector.class);
    }

    /**
     * Ensures the PomCollector binding works and returns a non-null implementation.
     */
    @Test
    @DisplayName("Guice creates PomCollector implementation")
    void collectorIsCreated() {
        assertNotNull(collector);
    }

    /**
     * Ensures collectAll returns an empty set for an empty directory.
     */
    @Test
    @DisplayName("collectAll: returns empty set for empty directory")
    void collectAllReturnsEmptySetForEmptyDirectory() {
        Set<Path> actual = collector.collectAll(tempDir.toString());
        assertTrue(actual.isEmpty());
    }

    /**
     * Ensures collectAll finds .pom files recursively and ignores non-.pom files.
     */
    @Test
    @DisplayName("collectAll: collects .pom recursively and ignores non-.pom")
    void collectAllCollectsPomRecursivelyAndIgnoresOthers() throws IOException {
        Path rootPom = writeFile(tempDir.resolve("a-1.0.pom"), "<project/>");
        writeFile(tempDir.resolve("a-1.0.jar"), "binary");

        Path nested = Files.createDirectories(tempDir.resolve("nested/dir"));
        Path nestedPom = writeFile(nested.resolve("b-2.0.pom"), "<project/>");
        writeFile(nested.resolve("readme.txt"), "txt");

        Set<Path> actual = collector.collectAll(tempDir.toString());

        assertAll(
                () -> assertTrue(actual.contains(rootPom), "must contain root .pom"),
                () -> assertTrue(actual.contains(nestedPom), "must contain nested .pom"),
                () -> assertEquals(2, actual.size(), "must contain only .pom files")
        );
    }

    /**
     * Ensures collectAll returns unique paths (Set semantics).
     */
    @Test
    @DisplayName("collectAll: result contains unique paths (Set semantics)")
    void collectAllReturnsUniquePaths() throws IOException {
        Path pom = writeFile(tempDir.resolve("dup-1.0.pom"), "<project/>");

        Set<Path> actual = collector.collectAll(tempDir.toString());

        assertAll(
                () -> assertTrue(actual.contains(pom)),
                () -> assertEquals(1, actual.size(), "single .pom should appear once")
        );
    }

    /**
     * Ensures collectAll wraps IO failures into RuntimeException.
     */
    @Test
    @DisplayName("collectAll: wraps IO errors into RuntimeException")
    void collectAllWrapsIoErrorsIntoRuntimeException() {
        Path missing = tempDir.resolve("does-not-exist");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> collector.collectAll(missing.toString())
        );

        assertNotNull(ex.getCause(), "cause must be present");
        assertInstanceOf(IOException.class, ex.getCause(), "cause must be an IOException");
    }

    /**
     * Ensures collectSelected returns an empty set if no .pom matches any prefix.
     */
    @Test
    @DisplayName("collectSelected: returns empty set when no filenames match prefixes")
    void collectSelectedReturnsEmptySetWhenNoMatches() throws IOException {
        writeFile(tempDir.resolve("a-1.0.pom"), "<project/>");
        writeFile(tempDir.resolve("b-1.0.pom"), "<project/>");

        Set<Path> actual = collector.collectSelected(tempDir.toString(), List.of("c"));

        assertTrue(actual.isEmpty());
    }

    /**
     * Ensures collectSelected matches by file name prefix (startsWith) and is recursive.
     */
    @Test
    @DisplayName("collectSelected: matches by filename prefix and is recursive")
    void collectSelectedMatchesByFilenamePrefixAndIsRecursive() throws IOException {
        Path a = writeFile(tempDir.resolve("commons-lang3-3.12.0.pom"), "<project/>");
        writeFile(tempDir.resolve("commons-io-2.11.0.pom"), "<project/>");

        Path nested = Files.createDirectories(tempDir.resolve("x/y"));
        Path nestedHit = writeFile(nested.resolve("commons-lang3-3.17.0.pom"), "<project/>");
        writeFile(nested.resolve("not-related-1.0.pom"), "<project/>");

        Set<Path> actual = collector.collectSelected(tempDir.toString(), List.of("commons-lang3"));

        assertAll(
                () -> assertTrue(actual.contains(a), "must include matching root pom"),
                () -> assertTrue(actual.contains(nestedHit), "must include matching nested pom"),
                () -> assertEquals(2, actual.size(), "must include only matched .pom files")
        );
    }

    /**
     * Ensures collectSelected ignores non-.pom files even if prefix matches.
     */
    @Test
    @DisplayName("collectSelected: ignores non-.pom files even if prefix matches")
    void collectSelectedIgnoresNonPomEvenIfPrefixMatches() throws IOException {
        writeFile(tempDir.resolve("aaa-1.0.jar"), "binary");
        Path pom = writeFile(tempDir.resolve("aaa-1.0.pom"), "<project/>");

        Set<Path> actual = collector.collectSelected(tempDir.toString(), List.of("aaa"));

        assertAll(
                () -> assertTrue(actual.contains(pom)),
                () -> assertEquals(1, actual.size(), "must not include .jar")
        );
    }

    /**
     * Ensures collectSelected returns empty set for an empty prefix list.
     */
    @Test
    @DisplayName("collectSelected: empty prefix list yields empty result")
    void collectSelectedEmptyPrefixListYieldsEmptyResult() throws IOException {
        writeFile(tempDir.resolve("a-1.0.pom"), "<project/>");

        Set<Path> actual = collector.collectSelected(tempDir.toString(), List.of());

        assertTrue(actual.isEmpty());
    }

    /**
     * Ensures collectSelected throws NullPointerException when artifactNames is null (current behavior).
     */
    @Test
    @DisplayName("collectSelected: rejects null artifactNames (current behavior)")
    void collectSelectedRejectsNullArtifactNames() {
        assertThrows(
                NullPointerException.class,
                () -> collector.collectSelected(tempDir.toString(), null)
        );
    }

    /**
     * Ensures collectSelected wraps IO failures into RuntimeException.
     */
    @Test
    @DisplayName("collectSelected: wraps IO errors into RuntimeException")
    void collectSelectedWrapsIoErrorsIntoRuntimeException() {
        Path missing = tempDir.resolve("does-not-exist");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> collector.collectSelected(missing.toString(), List.of("a"))
        );

        assertNotNull(ex.getCause(), "cause must be present");
        assertInstanceOf(IOException.class, ex.getCause(), "cause must be an IOException");
    }

    private static Path writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content);
    }
}
