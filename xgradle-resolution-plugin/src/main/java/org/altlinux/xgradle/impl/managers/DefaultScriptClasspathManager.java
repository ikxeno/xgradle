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

import org.gradle.api.artifacts.DependencyResolutionListener;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.initialization.Settings;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.logging.Logger;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Puts the system repository first in the {@code buildscript} repositories of the
 * settings script and of each project script, and resolves their classpath to
 * installed revisions. Gradle calls {@code beforeProject} before it evaluates the
 * project's script, so the repository is in place when the script's
 * {@code buildscript { }} block resolves.
 *
 * <p>Scripts applied with {@code apply from:} are not supported. Gradle gives each
 * of them a detached resolver with its own {@code buildscript} repositories and
 * has no public hook to reach it before it resolves. A resolution listener sees
 * the classpath only after its dependencies are frozen, so all the plugin can do
 * is warn.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultScriptClasspathManager implements ScriptClasspathManager {

    private final RepositoryManager repositoryManager;
    private final IvyRepositoryGenerator repositoryGenerator;
    private final DependencySubstitutor substitutor;
    private final MetadataIndex metadataIndex;
    private final Logger logger;
    private final Set<ResolvableDependencies> configuredClasspaths =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    @Inject
    DefaultScriptClasspathManager(
            RepositoryManager repositoryManager,
            IvyRepositoryGenerator repositoryGenerator,
            DependencySubstitutor substitutor,
            MetadataIndex metadataIndex,
            Logger logger
    ) {
        this.repositoryManager = repositoryManager;
        this.repositoryGenerator = repositoryGenerator;
        this.substitutor = substitutor;
        this.metadataIndex = metadataIndex;
        this.logger = logger;
    }

    @Override
    public void configure(Settings settings) {
        if (metadataIndex.artifacts().isEmpty()) {
            return;
        }
        IvyRepository repository = repositoryGenerator.generate(SystemDepsExtension.getIvyCacheDir(settings.getGradle()));
        configure(settings.getBuildscript(), repository);
        settings.getGradle().beforeProject(project -> configure(project.getBuildscript(), repository));
        settings.getGradle().addListener(new AppliedScriptClasspathWarning());
    }

    private void configure(ScriptHandler buildscript, IvyRepository repository) {
        repositoryManager.configureDependenciesRepository(buildscript.getRepositories(), repository);
        substitutor.configure(buildscript.getConfigurations());
        buildscript.getConfigurations().configureEach(configuration ->
                configuredClasspaths.add(configuration.getIncoming()));
    }

    /**
     * Warns about a script classpath this manager did not configure, which is the
     * classpath of a script applied with {@code apply from:}.
     */
    private final class AppliedScriptClasspathWarning implements DependencyResolutionListener {

        @Override
        public void beforeResolve(ResolvableDependencies dependencies) {
            if (!ScriptHandler.CLASSPATH_CONFIGURATION.equals(dependencies.getName())
                    || configuredClasspaths.contains(dependencies)) {
                return;
            }
            String modules = dependencies.getDependencies().stream()
                    .filter(ExternalModuleDependency.class::isInstance)
                    .map(dependency -> dependency.getGroup() + ":" + dependency.getName()
                            + ":" + dependency.getVersion())
                    .collect(Collectors.joining(", "));
            if (!modules.isEmpty()) {
                logger.warn("xgradle: buildscript classpath of a script applied with 'apply from' is not resolved "
                        + "from installed artifacts (not supported by the Gradle API): {}. "
                        + "Move it to the project's buildscript { } block or to plugins { }.", modules);
            }
        }

        @Override
        public void afterResolve(ResolvableDependencies dependencies) {
        }
    }
}
