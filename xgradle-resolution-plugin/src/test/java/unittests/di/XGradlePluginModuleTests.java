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
package unittests.di;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.altlinux.xgradle.impl.di.XGradlePluginModule;
import org.altlinux.xgradle.interfaces.handlers.PluginsDependenciesHandler;
import org.altlinux.xgradle.interfaces.handlers.ProjectDependenciesHandler;
import org.altlinux.xgradle.interfaces.resolution.SystemDependencyResolution;
import unittests.metadata.Installations;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("XGradlePluginModule contract")
class XGradlePluginModuleTests {

    @Test
    @DisplayName("Creates injector and resolves top-level handlers")
    void createsInjectorAndResolvesTopLevelHandlers() {
        Injector injector = Guice.createInjector(new XGradlePluginModule(Installations.metadataOnly(List.of(), true)));

        assertAll(
                () -> assertNotNull(injector.getInstance(SystemDependencyResolution.class)),
                () -> assertNotNull(injector.getInstance(ProjectDependenciesHandler.class)),
                () -> assertNotNull(injector.getInstance(PluginsDependenciesHandler.class))
        );
    }
}
