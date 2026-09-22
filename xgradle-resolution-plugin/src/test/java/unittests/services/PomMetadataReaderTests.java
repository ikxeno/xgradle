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
import org.altlinux.xgradle.impl.caches.CachesModule;
import org.altlinux.xgradle.impl.maven.MavenModule;
import org.altlinux.xgradle.impl.parsers.ParsersModule;
import org.altlinux.xgradle.impl.services.ServicesModule;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.services.PomMetadata;
import org.altlinux.xgradle.interfaces.services.PomMetadataReader;
import org.altlinux.xgradle.interfaces.services.VersionScanner;
import org.gradle.api.logging.Logger;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

/**
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PomMetadataReader contract")
class PomMetadataReaderTests {

    @Mock
    private Logger logger;

    @Mock
    private VersionScanner versionScanner;

    @Mock
    private PomFinder pomFinder;

    @Test
    @DisplayName("Reads project URL SCM URL and licenses")
    void readsMetadataFromPom() {
        Injector injector = createInjector();
        PomMetadataReader reader = injector.getInstance(PomMetadataReader.class);

        PomMetadata metadata = reader.read(
                Path.of("src/test/resources/poms/junit5/junit-jupiter.pom")
        );

        assertEquals("https://junit.org/junit5/", metadata.getProjectUrl());
        assertEquals("https://github.com/junit-team/junit5", metadata.getScmUrl());
        assertFalse(metadata.getLicenses().isEmpty());
        assertEquals("Eclipse Public License v2.0", metadata.getLicenses().get(0).getName());
    }

    @Test
    @DisplayName("Resolves placeholders in SCM URL")
    void resolvesScmUrlPlaceholders() {
        Injector injector = createInjector();
        PomMetadataReader reader = injector.getInstance(PomMetadataReader.class);

        PomMetadata metadata = reader.read(
                Path.of("src/test/resources/poms/maven-surefire/surefire.pom")
        );

        assertEquals(
                "https://github.com/apache/maven-surefire/tree/surefire-3.2.2",
                metadata.getScmUrl()
        );
    }

    private Injector createInjector() {
        return Guice.createInjector(
                Modules.override(
                        new CachesModule(),
                        new ParsersModule(),
                        new MavenModule(),
                        new ServicesModule()
                ).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Logger.class).toInstance(logger);
                        bind(MetadataIndex.class).toInstance(mock(MetadataIndex.class));
                        bind(VersionScanner.class).toInstance(versionScanner);
                        bind(PomFinder.class).toInstance(pomFinder);
                    }
                })
        );
    }
}
