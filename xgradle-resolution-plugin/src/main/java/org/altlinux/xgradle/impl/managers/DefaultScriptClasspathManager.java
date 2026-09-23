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

import org.altlinux.xgradle.impl.extensions.SystemDepsExtension;
import org.altlinux.xgradle.impl.model.IvyRepository;
import org.altlinux.xgradle.interfaces.managers.RepositoryManager;
import org.altlinux.xgradle.interfaces.managers.ScriptClasspathManager;
import org.altlinux.xgradle.interfaces.metadata.IvyRepositoryGenerator;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.resolvers.DependencySubstitutor;

import org.gradle.api.initialization.Settings;
import org.gradle.api.initialization.dsl.ScriptHandler;

/**
 * Puts the system repository first in the {@code buildscript} repositories of the
 * settings script and of each project script, and resolves their classpath to
 * installed revisions. Gradle calls {@code beforeProject} before it evaluates the
 * project's script, so the repository is in place when the script's
 * {@code buildscript { }} block resolves.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultScriptClasspathManager implements ScriptClasspathManager {

    private final RepositoryManager repositoryManager;
    private final IvyRepositoryGenerator repositoryGenerator;
    private final DependencySubstitutor substitutor;
    private final MetadataIndex metadataIndex;

    @Inject
    DefaultScriptClasspathManager(
            RepositoryManager repositoryManager,
            IvyRepositoryGenerator repositoryGenerator,
            DependencySubstitutor substitutor,
            MetadataIndex metadataIndex
    ) {
        this.repositoryManager = repositoryManager;
        this.repositoryGenerator = repositoryGenerator;
        this.substitutor = substitutor;
        this.metadataIndex = metadataIndex;
    }

    @Override
    public void configure(Settings settings) {
        if (metadataIndex.artifacts().isEmpty()) {
            return;
        }
        IvyRepository repository = repositoryGenerator.generate(SystemDepsExtension.getIvyCacheDir(settings.getGradle()));
        configure(settings.getBuildscript(), repository);
        settings.getGradle().beforeProject(project -> configure(project.getBuildscript(), repository));
    }

    private void configure(ScriptHandler buildscript, IvyRepository repository) {
        repositoryManager.configureDependenciesRepository(buildscript.getRepositories(), repository);
        substitutor.configure(buildscript.getConfigurations());
    }
}
