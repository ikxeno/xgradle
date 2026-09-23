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
package org.altlinux.xgradle.interfaces.maven;

import org.altlinux.xgradle.impl.model.MavenCoordinate;

import java.util.List;

/**
 * Finds installed modules by groupId and artifactId, as XMvn resolves a
 * request for the system version.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface PomFinder {

    /**
     * The installed module with its version, its packaging ({@code jar}, or
     * {@code pom} for a POM-only module) and the path of its POM if one is installed.
     *
     * @return the module, or null if it is not installed
     */
    MavenCoordinate findPomForArtifact(String groupId, String artifactId);

    /**
     * Every installed module of a group, by artifactId.
     */
    List<MavenCoordinate> findAllPomsForGroup(String groupId);
}
