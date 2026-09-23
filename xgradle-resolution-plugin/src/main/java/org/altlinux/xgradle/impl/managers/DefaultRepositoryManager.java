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
package org.altlinux.xgradle.impl.managers;

import com.google.inject.Inject;

import org.altlinux.xgradle.impl.model.IvyRepository;
import org.altlinux.xgradle.interfaces.managers.RepositoryManager;
import org.altlinux.xgradle.interfaces.metadata.IvyRepositoryGenerator;

import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;

/**
 * Adds the ivy repository generated from XMvn metadata to a Gradle build.
 * Implements {@link RepositoryManager}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class DefaultRepositoryManager implements RepositoryManager {

    private static final String PLUGINS_REPO_NAME = "SystemPluginsRepo";
    private static final String DEPENDENCIES_REPO_NAME = "SystemDepsRepo";

    private final Logger logger;

    @Inject
    DefaultRepositoryManager(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void configurePluginsRepository(Settings settings, IvyRepository repository) {
        addFirst(settings.getPluginManagement().getRepositories(), PLUGINS_REPO_NAME, repository);
    }

    @Override
    public void configureDependenciesRepository(RepositoryHandler repositories, IvyRepository repository) {
        addFirst(repositories, DEPENDENCIES_REPO_NAME, repository);
    }

    /**
     * Adds the repository and moves it to the front, because the repository DSL can
     * only append. System artifacts must win over every other repository.
     */
    private void addFirst(RepositoryHandler repositories, String name, IvyRepository repository) {
        IvyArtifactRepository ivy = repositories.ivy(repo -> {
            repo.setName(name);
            repo.setUrl(repository.getRoot().toUri());
            repo.patternLayout(layout -> {
                layout.ivy(IvyRepositoryGenerator.IVY_PATTERN);
                layout.artifact(IvyRepositoryGenerator.ARTIFACT_PATTERN);
            });
            repo.metadataSources(IvyArtifactRepository.MetadataSources::ivyDescriptor);
        });
        repositories.remove(ivy);
        repositories.addFirst(ivy);
        logger.info("Configured {} at {}", name, repository.getRoot());
    }
}
