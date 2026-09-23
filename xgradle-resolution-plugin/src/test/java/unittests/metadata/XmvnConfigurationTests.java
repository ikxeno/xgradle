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

import org.altlinux.xgradle.impl.metadata.XmvnConfiguration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads XMvn resolver settings with ALT's /usr/share/xmvn/configuration.xml.
 *
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("XMvn configuration")
class XmvnConfigurationTests {

    @TempDir
    Path temp;

    @Test
    @DisplayName("reads ALT's metadata repositories and XMvn's default for duplicates")
    void readsAltConfiguration() throws IOException {
        XmvnConfiguration xmvn = XmvnConfiguration.load(
                temp.resolve("project"), altEnvironment(), temp.resolve("home"), false);

        assertEquals(List.of(Path.of("/usr/share/maven-metadata"),
                        Path.of("/usr/share/javapackages-bootstrap/maven-metadata")),
                xmvn.getMetadataRepositories());
        assertTrue(xmvn.isIgnoreDuplicateMetadata());
    }

    @Test
    @DisplayName("puts project .xmvn settings before system ones and lets them override duplicates")
    void projectConfigurationComesFirst() throws IOException {
        Path project = temp.resolve("project");
        Path configD = Files.createDirectories(project.resolve(".xmvn/config.d"));
        Files.writeString(configD.resolve("10-local.xml"),
                "<configuration xmlns=\"http://fedorahosted.org/xmvn/CONFIG/2.0.0\"><resolverSettings>"
                        + "<metadataRepositories><repository>/opt/metadata</repository></metadataRepositories>"
                        + "<ignoreDuplicateMetadata>false</ignoreDuplicateMetadata>"
                        + "</resolverSettings></configuration>");

        XmvnConfiguration xmvn = XmvnConfiguration.load(project, altEnvironment(), temp.resolve("home"), false);

        assertEquals(Path.of("/opt/metadata"), xmvn.getMetadataRepositories().get(0));
        assertEquals(3, xmvn.getMetadataRepositories().size());
        assertFalse(xmvn.isIgnoreDuplicateMetadata());
    }

    @Test
    @DisplayName("treats empty XDG variables as unset and prefers $HOME to user.home")
    void emptyVariablesAndHome() throws IOException {
        Path userConfig = Files.createDirectories(temp.resolve("real-home/.config/xmvn"));
        Files.writeString(userConfig.resolve("configuration.xml"),
                "<configuration><resolverSettings><metadataRepositories>"
                        + "<repository>/home/metadata</repository></metadataRepositories>"
                        + "</resolverSettings></configuration>");
        Map<String, String> env = Map.of(
                "HOME", temp.resolve("real-home").toString(),
                "XDG_CONFIG_HOME", "",
                "XDG_DATA_DIRS", temp.resolve("share").toString(),
                "XDG_CONFIG_DIRS", "");

        XmvnConfiguration xmvn = XmvnConfiguration.load(temp.resolve("project"), env, temp.resolve("home"), false);

        assertEquals(List.of(Path.of("/home/metadata")), xmvn.getMetadataRepositories());
    }

    @Test
    @DisplayName("reads only the project configuration in XMvn's sandbox mode")
    void sandboxReadsOnlyProject() throws IOException {
        XmvnConfiguration xmvn = XmvnConfiguration.load(
                temp.resolve("project"), altEnvironment(), temp.resolve("home"), true);

        assertEquals(List.of(), xmvn.getMetadataRepositories());
    }

    @Test
    @DisplayName("resolves a relative metadata repository against the build directory")
    void relativeRepositoryIsFromBuildDirectory() throws IOException {
        Path project = temp.resolve("project");
        Path configD = Files.createDirectories(project.resolve(".xmvn/config.d"));
        Files.writeString(configD.resolve("10-local.xml"), "<configuration><resolverSettings>"
                + "<metadataRepositories><repository>local-metadata</repository></metadataRepositories>"
                + "</resolverSettings></configuration>");

        XmvnConfiguration xmvn = XmvnConfiguration.load(project, altEnvironment(), temp.resolve("home"), false);

        assertEquals(project.resolve("local-metadata"), xmvn.getMetadataRepositories().get(0));
    }

    private Map<String, String> altEnvironment() throws IOException {
        Path share = Files.createDirectories(temp.resolve("share/xmvn"));
        try (InputStream in = Objects.requireNonNull(
                getClass().getResourceAsStream("/xmvn-config/alt-configuration.xml"))) {
            Files.copy(in, share.resolve("configuration.xml"));
        }
        return Map.of(
                "XDG_DATA_DIRS", temp.resolve("share").toString(),
                "XDG_CONFIG_DIRS", temp.resolve("etc").toString());
    }
}
