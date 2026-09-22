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

import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.collectors.ConfigurationInfoCollector;
import org.altlinux.xgradle.impl.model.ConfigurationInfo;
import org.altlinux.xgradle.impl.model.ConfigurationInfoSnapshot;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.invocation.Gradle;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Collects declared dependency configuration usage across all projects.
 * Implements {@link ConfigurationInfoCollector}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultConfigurationInfoCollector implements ConfigurationInfoCollector {

    @Override
    public ConfigurationInfoSnapshot collect(Gradle gradle) {
        Map<String, Set<ConfigurationInfo>> dependencyConfigurations =
                new LinkedHashMap<>();
        Map<String, Boolean> testDependencyFlags =
                new LinkedHashMap<>();
        Map<String, Set<String>> dependencyConfigNames =
                new LinkedHashMap<>();

        gradle.allprojects(project ->
                collectFromProject(
                        project,
                        dependencyConfigurations,
                        testDependencyFlags,
                        dependencyConfigNames
                )
        );

        return new ConfigurationInfoSnapshot(
                dependencyConfigurations,
                testDependencyFlags,
                dependencyConfigNames
        );
    }

    private static void collectFromProject(
            Project project,
            Map<String, Set<ConfigurationInfo>> dependencyConfigurations,
            Map<String, Boolean> testDependencyFlags,
            Map<String, Set<String>> dependencyConfigNames
    ) {
        project.getConfigurations().all(configuration ->
                collectFromConfiguration(
                        configuration,
                        dependencyConfigurations,
                        testDependencyFlags,
                        dependencyConfigNames
                )
        );
    }

    private static void collectFromConfiguration(
            Configuration configuration,
            Map<String, Set<ConfigurationInfo>> dependencyConfigurations,
            Map<String, Boolean> testDependencyFlags,
            Map<String, Set<String>> dependencyConfigNames
    ) {
        ConfigurationInfo configurationInfo = new ConfigurationInfo(configuration);

        configuration.getDependencies().withType(ExternalModuleDependency.class).stream()
                .map(DefaultConfigurationInfoCollector::toDependencyKey)
                .filter(dependencyKey -> dependencyKey != null)
                .forEach(dependencyKey -> {
                    dependencyConfigurations
                            .computeIfAbsent(dependencyKey, key -> new HashSet<>())
                            .add(configurationInfo);

                    dependencyConfigNames
                            .computeIfAbsent(dependencyKey, key -> new HashSet<>())
                            .add(configuration.getName());

                    if (configurationInfo.hasTestConfiguration()) {
                        testDependencyFlags.put(dependencyKey, true);
                    } else {
                        testDependencyFlags.putIfAbsent(dependencyKey, false);
                    }
                });
    }

    private static String toDependencyKey(Dependency dependency) {
        String group = dependency.getGroup();
        String name = dependency.getName();

        if (group == null || group.trim().isEmpty()) {
            return null;
        }
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return group + ":" + name;
    }
}
