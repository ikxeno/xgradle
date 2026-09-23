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
package org.altlinux.xgradle.interfaces.services;

import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.gradle.api.invocation.Gradle;

import java.io.File;
import java.util.Collection;

/**
 * Generates SBOM reports from snapshots captured during resolution.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface SbomGenerationService {

    /**
     * @param artifacts       declared system dependencies
     * @param pluginArtifacts resolved Gradle plugins
     * @param resolvedJars    every jar the build resolved
     */
    void generate(
            Gradle gradle,
            SbomFormat format,
            Collection<MavenCoordinate> artifacts,
            Collection<MavenCoordinate> pluginArtifacts,
            Collection<File> resolvedJars
    );
}
