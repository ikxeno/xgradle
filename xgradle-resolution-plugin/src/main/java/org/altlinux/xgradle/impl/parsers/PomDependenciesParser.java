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

package org.altlinux.xgradle.impl.parsers;

import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.model.MavenCoordinateBuilder;
import org.apache.maven.model.Model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses direct dependencies from a Maven POM hierarchy and applies dependencyManagement rules.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class PomDependenciesParser {

    Map<String, MavenCoordinate> parse(
            List<Model> pomHierarchy,
            Map<String, String> properties,
            Map<String, MavenCoordinate> managedByGroupAndArtifact,
            PomPropertiesCollector propertiesCollector
    ) {
        Map<String, MavenCoordinate> resolvedByGroupAndArtifact =
                new LinkedHashMap<>();

        if (pomHierarchy == null) {
            return resolvedByGroupAndArtifact;
        }

        pomHierarchy.stream()
                .filter(model -> model != null && model.getDependencies() != null)
                .flatMap(model -> model.getDependencies().stream())
                .map(PomDependencyUtils::convertDependency)
                .map(coordinate -> PomDependencyUtils.resolveProperties(coordinate, properties, propertiesCollector))
                .map(coordinate -> applyDependencyManagement(coordinate, managedByGroupAndArtifact))
                .filter(PomDependenciesParser::hasModule)
                .forEach(coordinate -> resolvedByGroupAndArtifact.put(
                        coordinate.getGroupId() + ":" + coordinate.getArtifactId(),
                        coordinate
                ));
        return resolvedByGroupAndArtifact;
    }

    /**
     * A dependency needs a groupId and an artifactId. Its version may be missing: an
     * installed module is found without it, as XMvn falls back to the system version.
     */
    private static boolean hasModule(MavenCoordinate coordinate) {
        return coordinate.getGroupId() != null && !coordinate.getGroupId().isBlank()
                && coordinate.getArtifactId() != null && !coordinate.getArtifactId().isBlank();
    }

    private MavenCoordinate applyDependencyManagement(
            MavenCoordinate coordinate,
            Map<String, MavenCoordinate> managedByGroupAndArtifact
    ) {
        MavenCoordinate managed =
                managedByGroupAndArtifact.get(
                        coordinate.getGroupId()
                                + ":" + coordinate.getArtifactId()
                );

        if (managed == null) {
            return coordinate;
        }

        MavenCoordinateBuilder builder = coordinate.toBuilder();

        if (coordinate.getVersion() == null
                || coordinate.getVersion().isEmpty()) {
            builder.version(managed.getVersion());
        }

        if (coordinate.getScope() == null) {
            builder.scope(managed.getScope());
        }

        if (coordinate.getPackaging() == null
                || coordinate.getPackaging().isEmpty()) {
            builder.packaging(managed.getPackaging());
        }

        return builder.build();
    }
}
