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
import org.altlinux.xgradle.interfaces.handlers.PluginsDependenciesHandler;
import org.altlinux.xgradle.interfaces.handlers.ProjectDependenciesHandler;
import org.altlinux.xgradle.interfaces.metadata.InstalledArtifactsLoader;
import org.altlinux.xgradle.impl.extensions.SystemDepsExtension;
import org.altlinux.xgradle.impl.metadata.XmvnConfiguration;
import org.altlinux.xgradle.impl.di.XGradlePluginModule;
import org.altlinux.xgradle.impl.utils.config.XGradleConfig;

import org.altlinux.xgradle.impl.utils.ui.LogoPrinter;

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

        Injector injector = Guice.createInjector(
                new XGradlePluginModule()
        );
        XmvnConfiguration xmvn = XmvnConfiguration.load(gradle.getStartParameter().getCurrentDir().toPath());
        injector.getInstance(InstalledArtifactsLoader.class).load(
                SystemDepsExtension.getMetadataPaths(xmvn),
                xmvn.isIgnoreDuplicateMetadata(),
                SystemDepsExtension.getPomsDir(),
                SystemDepsExtension.getJavaDir());

        PluginsDependenciesHandler plugins = injector.getInstance(PluginsDependenciesHandler.class);
        ProjectDependenciesHandler dependencies = injector.getInstance(ProjectDependenciesHandler.class);

        gradle.beforeSettings(plugins::handle);
        gradle.projectsEvaluated(dependencies::handle);
    }

    private boolean isDisabled() {
        return "true".equalsIgnoreCase(XGradleConfig.getProperty("disable.xgradle", "false"));
    }

}
