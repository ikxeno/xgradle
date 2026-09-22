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
import org.altlinux.xgradle.interfaces.resolution.Order;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.altlinux.xgradle.interfaces.resolution.ResolvedArtifactsRegistry;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;

import java.io.File;
import java.util.Set;

/**
 * Registers listeners to capture resolved JAR files used by configurations.
 * Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
@Order(1100)
final class CollectResolvedJarsStep implements ResolutionStep {


    @Inject
    CollectResolvedJarsStep() {
    }

    @Override
    public String name() {
        return "collect-resolved-jars";
    }

    @Override
    public void execute(ResolutionContext resolutionContext) {
        Set<File> resolvedJars = ResolvedArtifactsRegistry.getOrCreate(
                resolutionContext.getGradle()
        );

        resolutionContext.getGradle().getRootProject().getAllprojects().forEach(project ->
                project.getConfigurations().stream()
                        .filter(Configuration::isCanBeResolved)
                        .forEach(configuration -> configuration.getIncoming().afterResolve(resolvable -> {
                            try {
                                configuration.getResolvedConfiguration().getResolvedArtifacts().stream()
                                        .map(ResolvedArtifact::getFile)
                                        .filter(file -> file != null && file.isFile() && isJar(file))
                                        .forEach(resolvedJars::add);
                            } catch (RuntimeException exception) {
                                project.getLogger().warn(
                                        "SBOM may be incomplete: cannot collect resolved jars of '{}': {}",
                                        configuration.getName(),
                                        exception.getMessage()
                                );
                            }
                        }))
        );
    }

    private boolean isJar(File file) {
        String name = file.getName();
        return name.endsWith(".jar");
    }
}
