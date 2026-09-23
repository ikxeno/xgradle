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
import org.altlinux.xgradle.interfaces.processors.PluginProcessor;
import org.altlinux.xgradle.interfaces.services.VersionScanner;
import org.gradle.api.Action;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.plugin.management.PluginManagementSpec;
import org.gradle.plugin.management.PluginRequest;
import org.gradle.plugin.management.PluginResolutionStrategy;
import org.gradle.plugin.management.PluginResolveDetails;
import org.gradle.plugin.use.PluginId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PluginProcessor contract")
class PluginProcessorTests {

    @Mock
    private VersionScanner scanner;

    @Mock
    private Logger logger;

    @Mock
    private Settings settings;

    @Mock
    private PluginManagementSpec pluginManagement;

    @Mock
    private PluginResolutionStrategy strategy;

    @Mock
    private PluginResolveDetails details;

    @Mock
    private PluginRequest request;

    @Mock
    private PluginId pluginId;

    @Test
    @DisplayName("Resolves plugin and uses module/version")
    void resolvesPlugin() {
        MavenCoordinate coord = MavenCoordinate.builder()
                .groupId("com.acme.plugin")
                .artifactId("plugin-artifact")
                .version("1.0")
                .pomPath(Path.of("p.pom"))
                .build();

        when(scanner.findPluginArtifact("com.acme.plugin")).thenReturn(coord);

        when(settings.getPluginManagement()).thenReturn(pluginManagement);
        when(pluginManagement.getResolutionStrategy()).thenReturn(strategy);

        when(details.getRequested()).thenReturn(request);
        when(request.getId()).thenReturn(pluginId);
        when(pluginId.getId()).thenReturn("com.acme.plugin");

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Action<PluginResolveDetails> action = invocation.getArgument(0);
            action.execute(details);
            return null;
        }).when(strategy).eachPlugin(any());

        Injector injector = Guice.createInjector(
                Modules.override(new ProcessorsModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(VersionScanner.class).toInstance(scanner);
                        bind(Logger.class).toInstance(logger);
                    }
                })
        );

        PluginProcessor processor = injector.getInstance(PluginProcessor.class);
        processor.process(settings);

        verify(details).useModule("com.acme.plugin:plugin-artifact:1.0");
        verify(details).useVersion("1.0");

        Collection<MavenCoordinate> resolved = processor.getResolvedPluginArtifacts();
        assertEquals(1, resolved.size());

        MavenCoordinate resolvedCoord = resolved.iterator().next();
        assertEquals("com.acme.plugin", resolvedCoord.getGroupId());
        assertEquals("plugin-artifact", resolvedCoord.getArtifactId());
        assertEquals("1.0", resolvedCoord.getVersion());
    }
}
