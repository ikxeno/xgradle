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
package org.altlinux.xgradle.interfaces.collectors;

import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.models.SbomComponent;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Collects SBOM components from resolved artifacts and build metadata.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface SbomComponentCollector {

    /**
     * @param artifacts       declared system dependencies
     * @param pluginArtifacts resolved Gradle plugins
     * @param resolvedJars    every jar the build resolved, transitive ones included
     */
    List<SbomComponent> collect(
            Collection<MavenCoordinate> artifacts,
            Collection<MavenCoordinate> pluginArtifacts,
            Collection<File> resolvedJars
    );
}
