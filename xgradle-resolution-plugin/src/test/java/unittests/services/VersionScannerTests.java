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
package unittests.services;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.services.ServicesModule;
import org.altlinux.xgradle.interfaces.maven.ModuleFinder;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.interfaces.services.PomMetadataReader;
import org.altlinux.xgradle.interfaces.services.VersionScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VersionScanner contract")
class VersionScannerTests {

    @Mock
    private ModuleFinder moduleFinder;

    @Mock
    private PomParser pomParser;

    @Mock
    private PomMetadataReader pomMetadataReader;

    @Test
    @DisplayName("Finds plugin artifact via standard variants")
    void findsPluginArtifact() {
        MavenCoordinate coord = MavenCoordinate.builder()
                .groupId("com.acme.plugin")
                .artifactId("com.acme.plugin.gradle.plugin")
                .version("1")
                .pomPath(Path.of("p.pom"))
                .build();

        when(moduleFinder.findModule("com.acme.plugin", "com.acme.plugin.gradle.plugin"))
                .thenReturn(Optional.of(coord));

        Injector injector = Guice.createInjector(
                Modules.override(new ServicesModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ModuleFinder.class).toInstance(moduleFinder);
                        bind(PomParser.class).toInstance(pomParser);
                        bind(PomMetadataReader.class).toInstance(pomMetadataReader);
                    }
                })
        );

        VersionScanner scanner = injector.getInstance(VersionScanner.class);
        MavenCoordinate result = scanner.findPluginArtifact("com.acme.plugin");

        assertEquals(coord, result);
    }
}
