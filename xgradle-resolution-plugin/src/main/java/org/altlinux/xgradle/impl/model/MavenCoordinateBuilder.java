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

import org.altlinux.xgradle.impl.enums.MavenPackaging;
import org.altlinux.xgradle.impl.enums.MavenScope;

import java.nio.file.Path;
/**
 * Builder for Maven Coordinate.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class MavenCoordinateBuilder {

    String groupId;
    String artifactId;
    String version;
    String packaging = MavenPackaging.JAR.getPackaging();
    MavenScope scope = MavenScope.COMPILE;
    Path pomPath;
    boolean optional;

    public MavenCoordinateBuilder() {
    }

    public MavenCoordinateBuilder(MavenCoordinate src) {
        this.groupId = src.getGroupId();
        this.artifactId = src.getArtifactId();
        this.version = src.getVersion();
        this.packaging = src.getPackaging();
        this.scope = src.getScope();
        this.pomPath = src.getPomPath();
        this.optional = src.isOptional();
    }

    public MavenCoordinateBuilder groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public MavenCoordinateBuilder artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public MavenCoordinateBuilder version(String version) {
        this.version = version;
        return this;
    }

    public MavenCoordinateBuilder packaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public MavenCoordinateBuilder scope(MavenScope scope) {
        this.scope = scope;
        return this;
    }

    public MavenCoordinateBuilder scope(String scope) {
        this.scope = MavenScope.fromScope(scope);
        return this;
    }

    public MavenCoordinateBuilder pomPath(Path path) {
        this.pomPath = path;
        return this;
    }


    public MavenCoordinateBuilder optional(boolean optional) {
        this.optional = optional;
        return this;
    }

    public MavenCoordinate build() {
        return new MavenCoordinate(this);
    }
}
