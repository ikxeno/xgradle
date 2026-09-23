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

import org.altlinux.xgradle.interfaces.managers.ProjectResolutionManager;
import org.altlinux.xgradle.interfaces.managers.RepositoryManager;
import org.altlinux.xgradle.interfaces.metadata.SystemRepository;
import org.altlinux.xgradle.interfaces.resolvers.DependencySubstitutor;

import org.gradle.api.initialization.Settings;

/**
 * Puts the system repository first in the settings repositories and in the
 * repositories of each project that declares its own, and resolves every
 * configuration to installed revisions. Both happen before any script runs, so a
 * configuration resolved while a script is evaluated is covered too.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultProjectResolutionManager implements ProjectResolutionManager {

    private final RepositoryManager repositoryManager;
    private final SystemRepository systemRepository;
    private final DependencySubstitutor substitutor;

    @Inject
    DefaultProjectResolutionManager(
            RepositoryManager repositoryManager,
            SystemRepository systemRepository,
            DependencySubstitutor substitutor
    ) {
        this.repositoryManager = repositoryManager;
        this.systemRepository = systemRepository;
        this.substitutor = substitutor;
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void configure(Settings settings) {
        systemRepository.forBuild(settings.getGradle()).ifPresent(repository -> {
            repositoryManager.configureDependenciesRepository(
                    settings.getDependencyResolutionManagement().getRepositories(), repository);
            settings.getGradle().beforeProject(project -> {
                repositoryManager.configureProjectRepository(project.getRepositories(), repository);
                substitutor.configure(project.getConfigurations());
            });
        });
    }
}
