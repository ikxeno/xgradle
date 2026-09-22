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
package org.altlinux.xgradle.impl.resolution;

import org.altlinux.xgradle.impl.model.MavenCoordinate;

import org.gradle.api.invocation.Gradle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Holds mutable resolution state for a single Gradle build invocation.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class ResolutionContext {

    private final Gradle gradle;

    private final Set<String> projectDependencies = new HashSet<>();
    private final Map<String, Set<String>> requestedVersions = new HashMap<>();

    private final Map<String, MavenCoordinate> systemArtifacts = new HashMap<>();
    private final Set<String> notFound = new HashSet<>();
    private final Set<String> skipped = new HashSet<>();

    private final Map<String, String> overrideLogs = new HashMap<>();

    public ResolutionContext(Gradle gradle) {
        this.gradle = gradle;
    }

    public Gradle getGradle() {
        return gradle;
    }

    public Set<String> getProjectDependencies() {
        return projectDependencies;
    }

    public Map<String, Set<String>> getRequestedVersions() {
        return requestedVersions;
    }

    public Map<String, MavenCoordinate> getSystemArtifacts() {
        return systemArtifacts;
    }

    public Set<String> getNotFound() {
        return notFound;
    }

    public Set<String> getSkipped() {
        return skipped;
    }

    public Map<String, String> getOverrideLogs() {
        return overrideLogs;
    }

    public void markNotFound(String dependencyKey) {
        if (dependencyKey != null && !dependencyKey.trim().isEmpty()) {
            notFound.add(dependencyKey);
        }
    }

    public void markSkipped(String dependencyKey) {
        if (dependencyKey != null && !dependencyKey.trim().isEmpty()) {
            skipped.add(dependencyKey);
        }
    }

    public void putSystemArtifact(String dependencyKey, MavenCoordinate coordinate) {
        if (dependencyKey != null && coordinate != null) {
            systemArtifacts.put(dependencyKey, coordinate);
        }
    }
}
