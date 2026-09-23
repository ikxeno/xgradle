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

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.invocation.Gradle;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jars the build resolves, transitive ones included, for the SBOM.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class ResolvedJars {

    private ResolvedJars() {
    }

    /**
     * Returns a set that receives the jars of each resolvable configuration of every
     * project when that configuration is resolved.
     */
    static Set<File> watch(Gradle gradle) {
        Set<File> jars = ConcurrentHashMap.newKeySet();
        gradle.getRootProject().getAllprojects().forEach(project ->
                project.getConfigurations().stream()
                        .filter(Configuration::isCanBeResolved)
                        .forEach(configuration -> configuration.getIncoming().afterResolve(resolvable -> {
                            try {
                                configuration.getResolvedConfiguration().getResolvedArtifacts().stream()
                                        .map(ResolvedArtifact::getFile)
                                        .filter(file -> file != null && file.isFile() && file.getName().endsWith(".jar"))
                                        .forEach(jars::add);
                            } catch (RuntimeException exception) {
                                project.getLogger().warn(
                                        "SBOM may be incomplete: cannot collect resolved jars of '{}': {}",
                                        configuration.getName(),
                                        exception.getMessage()
                                );
                            }
                        })));
        return jars;
    }
}
