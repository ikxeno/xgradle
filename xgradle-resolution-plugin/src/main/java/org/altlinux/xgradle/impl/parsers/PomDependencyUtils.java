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

import org.altlinux.xgradle.impl.enums.MavenPackaging;
import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.apache.maven.model.Dependency;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility methods for converting and resolving Maven dependencies.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class PomDependencyUtils {

    private PomDependencyUtils() {
    }

    static MavenCoordinate convertDependency(Dependency dependency) {
        return MavenCoordinate.builder()
                .groupId(dependency.getGroupId())
                .artifactId(dependency.getArtifactId())
                .version(dependency.getVersion())
                .scope(
                        dependency.getScope() != null
                                ? dependency.getScope()
                                : MavenScope.COMPILE.getScope()
                )
                .packaging(
                        dependency.getType() != null
                                ? dependency.getType()
                                : MavenPackaging.JAR.getPackaging()
                )
                .optional(Boolean.parseBoolean(dependency.getOptional()))
                .classifier(dependency.getClassifier())
                .exclusions(dependency.getExclusions().stream()
                        .map(exclusion -> exclusion.getGroupId() + ":" + exclusion.getArtifactId())
                        .collect(Collectors.toList()))
                .build();
    }

    static MavenCoordinate resolveProperties(
            MavenCoordinate coordinate,
            Map<String, String> properties,
            PomPropertiesCollector propertiesCollector
    ) {
        return coordinate.toBuilder()
                .groupId(
                        propertiesCollector.resolve(
                                coordinate.getGroupId(),
                                properties
                        )
                )
                .artifactId(
                        propertiesCollector.resolve(
                                coordinate.getArtifactId(),
                                properties
                        )
                )
                .version(
                        propertiesCollector.resolve(
                                coordinate.getVersion(),
                                properties
                        )
                )
                .packaging(
                        propertiesCollector.resolve(
                                coordinate.getPackaging(),
                                properties
                        )
                )
                .classifier(propertiesCollector.resolve(coordinate.getClassifier(), properties))
                .scope(
                        propertiesCollector.resolve(
                                coordinate.getScope() != null
                                        ? coordinate.getScope().getScope()
                                        : null,
                                properties
                        )
                )
                .build();
    }
}
