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
package org.altlinux.xgradle.impl.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.services.VersionScanner;
import org.altlinux.xgradle.impl.model.MavenCoordinate;

import java.util.*;

/**
 * Scans system artifacts to resolve Maven coordinates for project dependencies,
 * including Gradle plugins and dependencies.
 * Implements {@link VersionScanner}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DependencyVersionScanner implements VersionScanner {

    private final PomFinder pomFinder;

    @Inject
    DependencyVersionScanner(PomFinder pomFinder) {
        this.pomFinder = pomFinder;
    }

    @Override
    public MavenCoordinate findPluginArtifact(String pluginId) {
        String baseName = pluginId.contains(".") ?
                pluginId.substring(pluginId.lastIndexOf('.') + 1) : pluginId;

        String[] artifactIds = {
                pluginId + ".gradle.plugin", pluginId,
                baseName + "-plugin", "gradle-" + baseName,
                "gradle-" + baseName + "-plugin", baseName + "-gradle-plugin",
                "gradle-plugin-" + baseName, baseName + "-gradle"
        };

        MavenCoordinate found = Arrays.stream(artifactIds)
                .map(artifactId -> pomFinder.findPomForArtifact(pluginId, artifactId))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (found != null) {
            return found;
        }

        if (pluginId.contains(".")) {
            String withoutDomain = pluginId.substring(pluginId.indexOf('.') + 1).replace('.', '-');
            String[] extendedArtifactIds = {
                    withoutDomain, withoutDomain + "-plugin", "gradle-" + withoutDomain,
                    "gradle-" + withoutDomain + "-plugin", withoutDomain + "-gradle-plugin"
            };

            MavenCoordinate extendedFound = Arrays.stream(extendedArtifactIds)
                    .map(artifactId -> pomFinder.findPomForArtifact(pluginId, artifactId))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (extendedFound != null) {
                return extendedFound;
            }
        }

        return findMainArtifactForGroup(pluginId);
    }

    private MavenCoordinate findMainArtifactForGroup(String groupId) {
        List<MavenCoordinate> candidates = pomFinder.findAllPomsForGroup(groupId);
        return candidates.stream()
                .filter(coord -> coord.getArtifactId().contains("gradle") ||
                        coord.getArtifactId().contains("plugin"))
                .findFirst()
                .orElse(null);
    }
}
