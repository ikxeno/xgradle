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
package unittests.maven;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.altlinux.xgradle.impl.maven.MavenModule;
import org.altlinux.xgradle.impl.metadata.MetadataModule;
import unittests.metadata.Installations;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.parsers.ParsersModule;
import org.altlinux.xgradle.interfaces.maven.PomFinder;

import org.gradle.api.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link PomFinder} against XMvn metadata taken from ALT Sisyphus packages.
 *
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("PomFinder over XMvn metadata")
class PomFinderTests {

    private PomFinder finder;

    @BeforeEach
    void setUp() throws URISyntaxException {
        Path fixtures = Path.of(Objects.requireNonNull(getClass().getResource("/xmvn-metadata")).toURI());
        Injector injector = Guice.createInjector(
                new MetadataModule(Installations.metadataOnly(List.of(fixtures), true)),
                new MavenModule(), new ParsersModule(), new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Logger.class).toInstance(mock(Logger.class));
                    }
                });
        finder = injector.getInstance(PomFinder.class);
    }

    @Test
    @DisplayName("finds an installed jar with its version and POM path")
    void findsJar() {
        MavenCoordinate guava = finder.findPomForArtifact("com.google.guava", "guava");

        assertEquals("33.5.0-jre", guava.getVersion());
        assertEquals("jar", guava.getPackaging());
        assertEquals(Path.of("/usr/share/maven-poms/guava/guava.pom"), guava.getPomPath());
    }

    @Test
    @DisplayName("finds a POM-only module as a BOM")
    void findsPomOnlyModule() {
        assertTrue(finder.findPomForArtifact("com.google.guava", "guava-parent").isBom());
    }

    @Test
    @DisplayName("finds an artifact through its alias")
    void findsAlias() {
        MavenCoordinate alias = finder.findPomForArtifact("org.hamcrest", "hamcrest-core");

        assertEquals("hamcrest-core", alias.getArtifactId());
        assertEquals("3.0", alias.getVersion());
    }

    @Test
    @DisplayName("returns null for an artifact that is not installed")
    void returnsNullWhenMissing() {
        assertNull(finder.findPomForArtifact("org.example", "absent"));
    }

    @Test
    @DisplayName("lists the modules of a group sorted by artifactId")
    void listsGroup() {
        assertEquals(List.of("failureaccess", "guava", "guava-parent"),
                finder.findAllPomsForGroup("com.google.guava").stream()
                        .map(MavenCoordinate::getArtifactId)
                        .collect(Collectors.toList()));
    }
}
