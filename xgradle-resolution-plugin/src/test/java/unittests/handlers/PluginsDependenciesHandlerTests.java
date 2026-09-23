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
package unittests.handlers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.altlinux.xgradle.impl.handlers.HandlersModule;
import org.altlinux.xgradle.interfaces.handlers.PluginsDependenciesHandler;
import org.altlinux.xgradle.interfaces.handlers.ProjectDependenciesHandler;
import org.altlinux.xgradle.interfaces.managers.PluginManager;
import org.altlinux.xgradle.interfaces.managers.ScriptClasspathManager;
import org.gradle.api.initialization.Settings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PluginsDependenciesHandler contract")
class PluginsDependenciesHandlerTests {

    @Mock
    private PluginManager pluginManager;

    @Mock
    private ScriptClasspathManager scriptClasspathManager;

    @Mock
    private ProjectDependenciesHandler projectHandler;

    @Mock
    private Settings settings;

    @Test
    @DisplayName("Configures plugins and the buildscript classpath")
    void delegatesToPluginManager() {
        Injector injector = Guice.createInjector(
                Modules.override(new HandlersModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PluginManager.class).toInstance(pluginManager);
                        bind(ScriptClasspathManager.class).toInstance(scriptClasspathManager);
                        bind(ProjectDependenciesHandler.class).toInstance(projectHandler);
                    }
                })
        );

        PluginsDependenciesHandler handler = injector.getInstance(PluginsDependenciesHandler.class);
        handler.handle(settings);

        verify(pluginManager).configure(settings);
        verify(scriptClasspathManager).configure(settings);
        verifyNoMoreInteractions(pluginManager, scriptClasspathManager);
    }
}
