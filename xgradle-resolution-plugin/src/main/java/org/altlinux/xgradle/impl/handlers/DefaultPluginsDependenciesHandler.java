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
 * See the License  for the specific language governing permissions and
 * limitations under the License.
 */
package org.altlinux.xgradle.impl.handlers;

import com.google.inject.Inject;

import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.handlers.PluginsDependenciesHandler;
import org.altlinux.xgradle.interfaces.managers.PluginManager;
import org.altlinux.xgradle.interfaces.managers.ScriptClasspathManager;

import org.gradle.api.initialization.Settings;

/**
 * Central handler for managing plugin dependencies in Gradle builds.
 * Implements {@link PluginsDependenciesHandler}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPluginsDependenciesHandler implements PluginsDependenciesHandler {

    private final PluginManager pluginManager;
    private final ScriptClasspathManager scriptClasspathManager;

    @Inject
    DefaultPluginsDependenciesHandler(PluginManager pluginManager, ScriptClasspathManager scriptClasspathManager) {
        this.pluginManager = pluginManager;
        this.scriptClasspathManager = scriptClasspathManager;
    }

    /**
     * Resolves plugins requested in {@code plugins { }} and the {@code buildscript}
     * classpath of every script from installed artifacts.
     */
    public void handle(Settings settings) {
        pluginManager.configure(settings);
        scriptClasspathManager.configure(settings);
    }
}
