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
package org.altlinux.xgradle.impl.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.managers.PluginManager;
import org.altlinux.xgradle.interfaces.managers.RepositoryManager;
import org.altlinux.xgradle.interfaces.metadata.SystemRepository;
import org.altlinux.xgradle.interfaces.processors.PluginProcessor;

import org.gradle.api.initialization.Settings;


/**
 * Manages the configuration of plugin resolution for Gradle builds.
 * Implements {@link PluginManager}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPluginManager  implements PluginManager {

    private final RepositoryManager repositoryManager;
    private final SystemRepository systemRepository;
    private final PluginProcessor pluginProcessor;

    @Inject
    DefaultPluginManager(
            RepositoryManager repositoryManager,
            SystemRepository systemRepository,
            PluginProcessor pluginProcessor
    ) {
        this.repositoryManager = repositoryManager;
        this.systemRepository = systemRepository;
        this.pluginProcessor = pluginProcessor;
    }

    public void configure(Settings settings) {
        systemRepository.forBuild(settings.getGradle()).ifPresent(repository -> {
            repositoryManager.configurePluginsRepository(settings, repository);
            pluginProcessor.process(settings);
        });
    }
}
