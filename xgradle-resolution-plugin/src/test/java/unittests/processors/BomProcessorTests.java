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
package unittests.processors;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.processors.ProcessorsModule;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.interfaces.processors.BomProcessor;
import org.altlinux.xgradle.interfaces.processors.BomResult;
import org.altlinux.xgradle.interfaces.processors.PluginProcessor;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unittests.TestGradleUtils;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BomProcessor contract")
class BomProcessorTests {

    @Mock
    private PomFinder pomFinder;

    @Mock
    private PomParser pomParser;

    @Mock
    private PluginProcessor pluginProcessor;

    @Mock
    private Logger logger;

    @Mock
    private Gradle gradle;

    @Test
    @DisplayName("Processes BOM dependencies and returns managed versions")
    void processesBomDependencies() {
        MavenCoordinate bom = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("bom")
                .version("1")
                .packaging("pom")
                .pomPath(Path.of("bom.pom"))
                .build();

        MavenCoordinate dep = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("lib")
                .version("2")
                .pomPath(Path.of("lib.pom"))
                .build();

        when(pomFinder.findPomForArtifact("g", "bom")).thenReturn(bom);
        when(pomParser.parseDependencyManagement(bom.getPomPath())).thenReturn(List.of(dep));

        Injector injector = Guice.createInjector(
                Modules.override(new ProcessorsModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomFinder.class).toInstance(pomFinder);
                        bind(PomParser.class).toInstance(pomParser);
                        bind(PluginProcessor.class).toInstance(pluginProcessor);
                        bind(Logger.class).toInstance(logger);
                    }
                })
        );

        BomProcessor processor = injector.getInstance(BomProcessor.class);
        BomProcessor.Context ctx = new BomProcessor.Context(null, new HashSet<>(Set.of("g:bom")));

        BomResult result = processor.process(ctx);

        assertTrue(result.getProcessedBoms().contains("g:bom"));
        assertEquals("2", result.getManagedVersions().get("g:lib"));
        assertTrue(result.getBomManagedDeps().keySet().stream().anyMatch(k -> k.startsWith("g:bom:")));
    }

    @Test
    @DisplayName("Removes BOM dependencies from configurations")
    void removesBomsFromConfigurations() {
        Injector injector = Guice.createInjector(
                Modules.override(new ProcessorsModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomFinder.class).toInstance(pomFinder);
                        bind(PomParser.class).toInstance(pomParser);
                        bind(PluginProcessor.class).toInstance(pluginProcessor);
                        bind(Logger.class).toInstance(logger);
                    }
                })
        );

        BomProcessor processor = injector.getInstance(BomProcessor.class);

        Project root = TestGradleUtils.newJavaProject("root");
        root.getDependencies().add("implementation", "g:bom:1");
        root.getDependencies().add("implementation", "g:lib:1");

        TestGradleUtils.gradleWithProjects(gradle, root);
        processor.removeBomsFromConfigurations(gradle, Set.of("g:bom"));

        boolean hasBom = root.getConfigurations()
                .getByName("implementation")
                .getDependencies()
                .stream()
                .anyMatch(d -> "g".equals(d.getGroup()) && "bom".equals(d.getName()));

        assertFalse(hasBom);
    }
}
