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
package unittests.managers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.util.Modules;
import org.altlinux.xgradle.impl.managers.ManagersModule;
import org.altlinux.xgradle.interfaces.managers.PluginManager;
import org.altlinux.xgradle.interfaces.managers.RepositoryManager;
import org.altlinux.xgradle.interfaces.processors.PluginProcessor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.altlinux.xgradle.impl.model.IvyRepository;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.interfaces.metadata.IvyRepositoryGenerator;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.gradle.api.invocation.Gradle;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PluginManager contract")
class PluginManagerTests {

    @Mock
    private RepositoryManager repoManager;

    @Mock
    private IvyRepositoryGenerator generator;

    @Mock
    private MetadataIndex index;

    @Mock
    private PluginProcessor pluginProcessor;

    @Mock
    private Logger logger;

    @Mock
    private Settings settings;

    @Mock
    private Gradle gradle;

    private PluginManager manager;

    @BeforeEach
    void setUp() {
        manager = Guice.createInjector(
                Modules.override(new ManagersModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(RepositoryManager.class).toInstance(repoManager);
                        bind(IvyRepositoryGenerator.class).toInstance(generator);
                        bind(MetadataIndex.class).toInstance(index);
                        bind(PluginProcessor.class).toInstance(pluginProcessor);
                        bind(Logger.class).toInstance(logger);
                    }
                })
        ).getInstance(PluginManager.class);
    }

    @Test
    @DisplayName("Configures the system repository and processes plugins when artifacts are installed")
    void configuresWhenArtifactsInstalled(@TempDir Path tempDir) {
        IvyRepository repository = new IvyRepository(tempDir);
        when(index.artifacts()).thenReturn(List.of(mock(XmvnArtifact.class)));
        when(settings.getGradle()).thenReturn(gradle);
        when(gradle.getGradleUserHomeDir()).thenReturn(tempDir.toFile());
        when(generator.generate(tempDir.resolve("caches/xgradle/ivy"))).thenReturn(repository);

        manager.configure(settings);

        verify(repoManager).configurePluginsRepository(settings, repository);
        verify(pluginProcessor).process(settings);
    }

    @Test
    @DisplayName("Warns and skips when no artifacts are installed")
    void skipsWithoutArtifacts() {
        when(index.artifacts()).thenReturn(List.of());

        manager.configure(settings);

        verify(logger).warn(startsWith("No installed artifacts found"));
        verifyNoInteractions(repoManager, pluginProcessor, generator);
    }
}
