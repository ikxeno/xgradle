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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Finds installed modules by groupId and artifactId, as XMvn resolves a
 * request for the system version.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface ModuleFinder {

    /**
     * The installed module with its version, its packaging ({@code jar}, or
     * {@code pom} for a POM-only module) and the path of its POM if one is installed.
     */
    default Optional<MavenCoordinate> findModule(String groupId, String artifactId) {
        return findModule(groupId, artifactId, List.of());
    }

    /**
     * Like {@link #findModule(String, String)}, for a dependency declared with the given
     * versions: a compat version matching one of them wins over the system version.
     */
    Optional<MavenCoordinate> findModule(String groupId, String artifactId, Collection<String> requestedVersions);

    /**
     * Every installed module of a group, by artifactId.
     */
    List<MavenCoordinate> findModulesOfGroup(String groupId);
}
