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
package org.altlinux.xgradle.impl.processors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.interfaces.processors.BomProcessor;
import org.altlinux.xgradle.interfaces.processors.BomResult;
import org.altlinux.xgradle.impl.model.MavenCoordinate;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Processes BOM (Bill of Materials) dependencies in Gradle projects.
 * Implements {@link BomProcessor}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultBomProcessor implements BomProcessor {
    private final PomFinder pomFinder;
    private final PomParser pomParser;
    private final Logger logger;

    @Inject
    DefaultBomProcessor(PomFinder pomFinder, PomParser pomParser, Logger logger) {
        this.pomFinder = pomFinder;
        this.pomParser = pomParser;
        this.logger = logger;
    }

    @Override
    public BomResult process(Context context) {
        if (context == null || context.getProjectDependencies() == null) {
            return BomResult.empty();
        }

        Map<String, List<String>> bomManagedDeps = new LinkedHashMap<>();
        Set<String> processedBoms = new HashSet<>();
        Map<String, String> managedVersions = new HashMap<>();

        Set<String> projectDependencies = context.getProjectDependencies();
        Queue<MavenCoordinate> bomQueue = new LinkedList<>();

        projectDependencies.stream()
                .filter(dep -> dep != null)
                .map(dep -> dep.split(":"))
                .filter(parts -> parts.length >= 2)
                .map(parts -> pomFinder.findPomForArtifact(parts[0], parts[1]))
                .filter(coord -> coord != null && coord.isBom())
                .forEach(coord -> {
                    bomQueue.add(coord);
                    processedBoms.add(coord.getGroupId() + ":" + coord.getArtifactId());
                });

        while (!bomQueue.isEmpty()) {
            MavenCoordinate bom = bomQueue.poll();

            String bomVersion = bom.getVersion() == null ? "unknown" : bom.getVersion();
            String bomKey = bom.getGroupId() + ":" + bom.getArtifactId() + ":" + bomVersion;

            List<String> managedDeps = new ArrayList<>();
            List<MavenCoordinate> dependencies = pomParser.parseDependencyManagement(bom.getPomPath());

            dependencies.stream().forEach(dependencyCoordinate -> {
                String dependencyId = dependencyCoordinate.getGroupId() + ":" + dependencyCoordinate.getArtifactId();

                projectDependencies.add(dependencyId);

                String dependencyVersion = dependencyCoordinate.getVersion();
                if (dependencyVersion != null && !dependencyVersion.isBlank()) {
                    managedVersions.put(dependencyId, dependencyVersion);
                    managedDeps.add(dependencyId + ":" + dependencyVersion);
                } else {
                    managedDeps.add(dependencyId);
                }

                if (dependencyCoordinate.isBom() && processedBoms.add(dependencyId)) {
                    bomQueue.add(dependencyCoordinate);
                }
            });

            bomManagedDeps.put(bomKey, managedDeps);
        }

        return new BomResult(bomManagedDeps, managedVersions, processedBoms);
    }

    @Override
    public void removeBomsFromConfigurations(Gradle gradle, Set<String> processedBoms) {
        if (gradle == null || processedBoms == null || processedBoms.isEmpty()) {
            return;
        }

        gradle.allprojects(project -> project.getConfigurations().all(cfg -> {
            List<Dependency> toRemove = cfg.getDependencies().withType(ExternalModuleDependency.class).stream()
                    .filter(dependency -> dependency.getGroup() != null)
                    .filter(dependency -> processedBoms.contains(dependency.getGroup() + ":" + dependency.getName()))
                    .collect(java.util.stream.Collectors.toList());

            toRemove.forEach(dependency -> cfg.getDependencies().remove(dependency));
        }));
    }
}
