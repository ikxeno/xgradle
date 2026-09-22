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

import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.model.ConfigurationInfoSnapshot;
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
    private final Set<String> allDependencies = new HashSet<>();
    private final Map<String, Set<String>> requestedVersions = new HashMap<>();
    private final Map<String, MavenScope> dependencyScopes = new HashMap<>();
    private final Map<String, String> managedVersions = new HashMap<>();
    private final Map<String, Set<String>> resolvedConfigNames = new HashMap<>();

    private ConfigurationInfoSnapshot configurationInfoSnapshot;

    private final Set<String> testContextDependencies = new HashSet<>();

    private final Map<String, MavenCoordinate> systemArtifacts = new HashMap<>();
    private final Set<String> notFound = new HashSet<>();
    private final Set<String> skipped = new HashSet<>();

    private final Map<String, String> overrideLogs = new HashMap<>();
    private final Map<String, String> applyLogs = new HashMap<>();


    public ResolutionContext(Gradle gradle) {
        this.gradle = gradle;
    }

    public Gradle getGradle() {
        return gradle;
    }

    public Set<String> getProjectDependencies() {
        return projectDependencies;
    }

    public Set<String> getAllDependencies() {
        return allDependencies;
    }

    public Map<String, Set<String>> getRequestedVersions() {
        return requestedVersions;
    }

    public Map<String, MavenScope> getDependencyScopes() {
        return dependencyScopes;
    }

    public Map<String, String> getManagedVersions() {
        return managedVersions;
    }

    public Map<String, Set<String>> getResolvedConfigNames() {
        return resolvedConfigNames;
    }

    public void setManagedVersions(Map<String, String> managedVersions) {
        this.managedVersions.clear();
        if (managedVersions != null && !managedVersions.isEmpty()) {
            this.managedVersions.putAll(managedVersions);
        }
    }

    public ConfigurationInfoSnapshot getConfigurationInfoSnapshot() {
        return configurationInfoSnapshot;
    }

    public void setConfigurationInfoSnapshot(ConfigurationInfoSnapshot configurationInfoSnapshot) {
        this.configurationInfoSnapshot = configurationInfoSnapshot;
    }

    public Set<String> getTestContextDependencies() {
        return testContextDependencies;
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

    public Map<String, String> getApplyLogs() {
        return applyLogs;
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
