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
package org.altlinux.xgradle.impl.plugin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import org.altlinux.xgradle.interfaces.handlers.PluginsDependenciesHandler;
import org.altlinux.xgradle.interfaces.handlers.ProjectDependenciesHandler;
import org.altlinux.xgradle.impl.extensions.SystemDepsExtension;
import org.altlinux.xgradle.impl.metadata.XmvnConfiguration;
import org.altlinux.xgradle.impl.model.InstalledLayout;
import org.altlinux.xgradle.impl.di.XGradlePluginModule;
import org.altlinux.xgradle.impl.utils.config.XGradleConfig;

import org.altlinux.xgradle.impl.utils.ui.LogoPrinter;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.invocation.Gradle;

import org.jetbrains.annotations.NotNull;


/**
 * Class implements {@link Plugin} interface.
 * <p>Core plugin implementation that applies to Gradle itself rather than individual projects.
 * Implements {@link Plugin}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class XGradlePlugin implements Plugin<Gradle> {

    @Override
    public void apply(@NotNull Gradle gradle) {
        XGradleConfig.initSystemProperties();
        if (isDisabled()) {
            return;
        }
        if (LogoPrinter.isLogoEnabled(gradle)) {
            LogoPrinter.printCenteredBanner();
        }

        XmvnConfiguration xmvn = XmvnConfiguration.load(gradle.getStartParameter().getCurrentDir().toPath());
        InstalledLayout layout = new InstalledLayout(
                SystemDepsExtension.getMetadataPaths(xmvn),
                xmvn.isIgnoreDuplicateMetadata(),
                SystemDepsExtension.getPomsDir(),
                SystemDepsExtension.getJavaDir());
        Injector injector = Guice.createInjector(new XGradlePluginModule(layout));

        PluginsDependenciesHandler plugins = instance(injector, PluginsDependenciesHandler.class);
        ProjectDependenciesHandler dependencies = instance(injector, ProjectDependenciesHandler.class);

        gradle.beforeSettings(plugins::handle);
        gradle.projectsEvaluated(dependencies::handle);
    }

    /**
     * Creating a handler builds the metadata index. A GradleException from that, such
     * as a missing metadata location, is rethrown as is instead of wrapped by Guice.
     */
    private static <T> T instance(Injector injector, Class<T> type) {
        try {
            return injector.getInstance(type);
        } catch (ProvisionException e) {
            if (e.getCause() instanceof GradleException) {
                throw (GradleException) e.getCause();
            }
            throw e;
        }
    }

    private boolean isDisabled() {
        return "true".equalsIgnoreCase(XGradleConfig.getProperty("disable.xgradle", "false"));
    }

}
