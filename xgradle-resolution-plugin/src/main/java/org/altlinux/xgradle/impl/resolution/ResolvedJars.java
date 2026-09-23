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

import org.altlinux.xgradle.interfaces.collectors.ResolvedJarsCollector;

import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adds the jars of each resolvable configuration to a set when the configuration
 * is resolved, including configurations created after the watch starts.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class ResolvedJars implements ResolvedJarsCollector {

    private final Set<File> jars = ConcurrentHashMap.newKeySet();
    private final Logger logger;

    @Inject
    ResolvedJars(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void watch(Settings settings) {
        if (!GenerateSbomStep.isRequested()) {
            return;
        }
        watch(settings.getBuildscript().getConfigurations());
        settings.getGradle().beforeProject(project -> {
            watch(project.getBuildscript().getConfigurations());
            watch(project.getConfigurations());
        });
    }

    @Override
    public Set<File> jars() {
        return Collections.unmodifiableSet(jars);
    }

    void watch(ConfigurationContainer configurations) {
        configurations.configureEach(configuration -> configuration.getIncoming().afterResolve(resolvable -> {
            if (!configuration.isCanBeResolved()) {
                return;
            }
            try {
                configuration.getResolvedConfiguration().getResolvedArtifacts().stream()
                        .map(ResolvedArtifact::getFile)
                        .filter(file -> file != null && file.isFile() && file.getName().endsWith(".jar"))
                        .forEach(jars::add);
            } catch (RuntimeException exception) {
                logger.warn("SBOM may be incomplete: cannot collect resolved jars of '{}': {}",
                        configuration.getName(), exception.getMessage());
            }
        }));
    }
}
