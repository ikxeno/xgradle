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
package org.altlinux.xgradle.impl.resolution;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.managers.RepositoryManager;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.altlinux.xgradle.interfaces.resolution.Order;
import org.altlinux.xgradle.impl.model.IvyRepository;
import org.altlinux.xgradle.interfaces.metadata.IvyRepositoryGenerator;

/**
 * Configures the system dependency repository for all projects in the build.
 * Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
@Order(100)
final class ConfigureSystemRepositoryStep implements ResolutionStep {

    private final RepositoryManager repositoryManager;
    private final IvyRepositoryGenerator repositoryGenerator;

    @Inject
    ConfigureSystemRepositoryStep(RepositoryManager repositoryManager, IvyRepositoryGenerator repositoryGenerator) {
        this.repositoryManager = repositoryManager;
        this.repositoryGenerator = repositoryGenerator;
    }

    @Override
    public String name() {
        return "configure-system-repository";
    }

    @Override
    public void execute(ResolutionContext ctx) {
        IvyRepository repository = repositoryGenerator.generate(ctx.getGradle());
        ctx.getGradle().allprojects(project ->
                repositoryManager.configureDependenciesRepository(project.getRepositories(), repository)
        );
    }
}
