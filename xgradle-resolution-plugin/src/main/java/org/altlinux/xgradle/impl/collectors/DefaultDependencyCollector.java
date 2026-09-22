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
package org.altlinux.xgradle.impl.collectors;

import org.altlinux.xgradle.interfaces.collectors.DependencyCollector;

import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.invocation.Gradle;

import java.util.*;

/**
 * Default implementation of {@link DependencyCollector} that inspects all projects in a Gradle build
 * and gathers declared dependencies.
 * Implements {@link DependencyCollector}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class DefaultDependencyCollector implements DependencyCollector {
    private final Map<String, Set<String>> requestedVersions = new HashMap<>();
    private final Set<String> dependencies = new LinkedHashSet<>();

    @Override
    public Set<String> collect(Gradle gradle) {
        gradle.allprojects(project -> project.getConfigurations().all(cfg ->
                cfg.getDependencies().withType(ExternalModuleDependency.class).stream()
                        .filter(dependency -> dependency.getGroup() != null)
                        .forEach(dependency -> {
                            String key = dependency.getGroup() + ":" + dependency.getName();
                            dependencies.add(key);
                            requestedVersions
                                    .computeIfAbsent(key, dependencyKey -> new HashSet<>())
                                    .add(dependency.getVersion());
                        })
        ));
        return dependencies;
    }

    @Override
    public Map<String, Set<String>> getRequestedVersions() {
        return requestedVersions;
    }

}
