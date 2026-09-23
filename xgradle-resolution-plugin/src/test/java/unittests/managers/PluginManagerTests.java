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
import org.altlinux.xgradle.interfaces.resolvers.DependencySubstitutor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.altlinux.xgradle.impl.model.IvyRepository;
import org.altlinux.xgradle.interfaces.metadata.SystemRepository;
import org.gradle.api.invocation.Gradle;
import org.junit.jupiter.api.BeforeEach;
import java.util.Optional;
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
    private SystemRepository systemRepository;

    @Mock
    private PluginProcessor pluginProcessor;

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
                        bind(SystemRepository.class).toInstance(systemRepository);
                        bind(PluginProcessor.class).toInstance(pluginProcessor);
                        bind(DependencySubstitutor.class).toInstance(mock(DependencySubstitutor.class));
                        bind(Logger.class).toInstance(mock(Logger.class));
                    }
                })
        ).getInstance(PluginManager.class);
    }

    @Test
    @DisplayName("Configures the system repository and processes plugins when artifacts are installed")
    void configuresWhenArtifactsInstalled(@TempDir Path tempDir) {
        IvyRepository repository = new IvyRepository(tempDir);
        when(settings.getGradle()).thenReturn(gradle);
        when(systemRepository.forBuild(gradle)).thenReturn(Optional.of(repository));

        manager.configure(settings);

        verify(repoManager).configurePluginsRepository(settings, repository);
        verify(pluginProcessor).process(settings);
    }

    @Test
    @DisplayName("Skips when no artifacts are installed")
    void skipsWithoutArtifacts() {
        when(settings.getGradle()).thenReturn(gradle);
        when(systemRepository.forBuild(gradle)).thenReturn(Optional.empty());

        manager.configure(settings);

        verifyNoInteractions(repoManager, pluginProcessor);
    }
}
