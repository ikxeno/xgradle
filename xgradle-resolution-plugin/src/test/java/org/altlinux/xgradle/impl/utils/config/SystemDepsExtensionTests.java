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
package org.altlinux.xgradle.impl.utils.config;

import org.altlinux.xgradle.impl.extensions.SystemDepsExtension;
import org.altlinux.xgradle.impl.metadata.XmvnConfiguration;

import unittests.Fixtures;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("SystemDepsExtension contract")
class SystemDepsExtensionTests {

    private static final String KEY = "maven.metadata.dir";
    private static final Path DEFAULT_DIR = Path.of("/usr/share/maven-metadata");

    @TempDir
    Path tempDir;

    private String prevHome;
    private String prevMetadataDir;

    @BeforeEach
    void setUp() throws Exception {
        prevHome = System.getProperty("user.home");
        prevMetadataDir = System.getProperty(KEY);

        XGradleConfig.resetForTests();
        System.setProperty("user.home", tempDir.toString());
        System.clearProperty(KEY);
        Files.createDirectories(tempDir.resolve(".xgradle"));
    }

    @AfterEach
    void tearDown() {
        restoreProperty("user.home", prevHome);
        restoreProperty(KEY, prevMetadataDir);
        XGradleConfig.resetForTests();
    }

    @Test
    @DisplayName("Prefers system property over config")
    void prefersSystemProperty() throws Exception {
        writeConfig(KEY + "=/tmp/from-config\n");
        System.setProperty(KEY, "/tmp/from-system");

        assertEquals(List.of(Path.of("/tmp/from-system")), SystemDepsExtension.getMetadataPaths(noXmvn()));
    }

    @Test
    @DisplayName("Reads config when system property is missing")
    void readsConfigWhenSystemMissing() throws Exception {
        writeConfig(KEY + "=/tmp/from-config\n");

        assertEquals(List.of(Path.of("/tmp/from-config")), SystemDepsExtension.getMetadataPaths(noXmvn()));
    }

    @Test
    @DisplayName("Splits, trims and deduplicates comma-separated paths")
    void parsesMultiplePaths() {
        System.setProperty(KEY, " /a , /b,/a ,");

        assertEquals(List.of(Path.of("/a"), Path.of("/b")), SystemDepsExtension.getMetadataPaths(noXmvn()));
    }

    @Test
    @DisplayName("Splits comma-separated java.library.dir into jar roots")
    void parsesMultipleJavaDirs() {
        String previous = System.getProperty("java.library.dir");
        System.setProperty("java.library.dir", "/usr/share/java,/usr/lib/java");
        try {
            assertEquals(List.of(Path.of("/usr/share/java"), Path.of("/usr/lib/java")),
                    SystemDepsExtension.getJavaDirs());
        } finally {
            restoreProperty("java.library.dir", previous);
        }
    }

    @Test
    @DisplayName("Falls back to /usr/share/maven-metadata only if it exists")
    void fallsBackToDefault() {
        List<Path> expected = Files.isDirectory(DEFAULT_DIR) ? List.of(DEFAULT_DIR) : List.of();

        assertEquals(expected, SystemDepsExtension.getMetadataPaths(noXmvn()));
    }

    @Test
    @DisplayName("Uses the XMvn metadata repositories that exist")
    void usesExistingXmvnRepositories() throws Exception {
        Path metadata = Files.createDirectories(tempDir.resolve("maven-metadata"));
        Fixtures.copy("xmvn-config/system-repositories", tempDir.resolve("share/xmvn"), tempDir);
        XmvnConfiguration xmvn = XmvnConfiguration.load(tempDir.resolve("project"),
                Map.of("XDG_DATA_DIRS", tempDir.resolve("share").toString(), "XDG_CONFIG_DIRS", "/nonexistent"),
                tempDir, false);

        assertEquals(List.of(metadata), SystemDepsExtension.getMetadataPaths(xmvn));
    }

    private XmvnConfiguration noXmvn() {
        return XmvnConfiguration.load(tempDir.resolve("project"),
                Map.of("XDG_DATA_DIRS", "/nonexistent", "XDG_CONFIG_DIRS", "/nonexistent"), tempDir, false);
    }

    private void writeConfig(String content) throws Exception {
        Path config = tempDir.resolve(".xgradle").resolve("xgradle.config");
        Files.writeString(config, content);
        XGradleConfig.resetForTests();
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
