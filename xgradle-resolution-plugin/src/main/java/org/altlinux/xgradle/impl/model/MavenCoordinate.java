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
package org.altlinux.xgradle.impl.model;

import org.altlinux.xgradle.impl.enums.MavenScope;

import java.nio.file.Path;
import java.util.Objects;
/**
 * Implementation for Maven Coordinate.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class MavenCoordinate {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String packaging;
    private final MavenScope scope;
    private final Path pomPath;
    private final boolean optional;

    MavenCoordinate(MavenCoordinateBuilder builder) {
        this.groupId = builder.groupId;
        this.artifactId = builder.artifactId;
        this.version = builder.version;
        this.packaging = builder.packaging;
        this.scope = builder.scope;
        this.pomPath = builder.pomPath;
        this.optional = builder.optional;
    }

    public static MavenCoordinateBuilder builder() {
        return new MavenCoordinateBuilder();
    }

    public MavenCoordinateBuilder toBuilder() {
        return new MavenCoordinateBuilder(this);
    }

    public boolean isValid() {
        return notEmpty(groupId)
                && notEmpty(artifactId)
                && notEmpty(version);
    }

    public boolean isBom() {
        return "pom".equals(packaging);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getPackaging() {
        return packaging;
    }

    public MavenScope getScope() {
        return scope;
    }

    public Path getPomPath() {
        return pomPath;
    }


    /**
     * Whether this coordinate is an optional dependency; Maven does not follow those transitively.
     */
    public boolean isOptional() {
        return optional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MavenCoordinate)) return false;
        MavenCoordinate that = (MavenCoordinate) o;
        return Objects.equals(groupId, that.groupId)
                && Objects.equals(artifactId, that.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.isBlank();
    }
}
