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
package org.altlinux.xgradle.impl.processors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.processors.PluginProcessor;
import org.altlinux.xgradle.interfaces.services.VersionScanner;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.plugin.management.PluginResolveDetails;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.altlinux.xgradle.impl.utils.logging.LogPainter.green;

/**
 * Processes plugin dependency resolution with support for BOM (Bill of Materials) packages.
 * Implements {@link PluginProcessor}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPluginProcessor implements PluginProcessor {

    private final VersionScanner versionScanner;
    private final Logger logger;
    private final Map<String, MavenCoordinate> resolvedPluginArtifacts = new LinkedHashMap<>();

    @Inject
    DefaultPluginProcessor(VersionScanner versionScanner, Logger logger) {
        this.versionScanner = versionScanner;
        this.logger = logger;
    }

    @Override
    public void process(Settings settings) {
        resolvedPluginArtifacts.clear();
        settings.getPluginManagement()
                .getResolutionStrategy()
                .eachPlugin(this::processPlugin);
    }

    private void processPlugin(PluginResolveDetails requested) {
        String pluginId = requested.getRequested().getId().getId();
        if (pluginId.startsWith("org.gradle.") || !pluginId.contains(".")) {
            logger.lifecycle("Skipping core plugin: {}", pluginId);
            return;
        }

        MavenCoordinate coord = versionScanner.findPluginArtifact(pluginId);
        if (coord != null && coord.isValid()) {
            usePlugin(requested, coord);
        } else {
            logger.warn("Plugin not resolved: {}", pluginId);
        }
    }



    private void usePlugin(PluginResolveDetails requested, MavenCoordinate coord) {
        String module = coord.getGroupId() + ":" + coord.getArtifactId() + ":" + coord.getVersion();
        requested.useModule(module);
        requested.useVersion(coord.getVersion());
        rememberResolvedPlugin(coord);
        logger.lifecycle(green("Resolved plugin: {} -> {}"), requested.getRequested().getId().getId(), module);
    }

    @Override
    public Collection<MavenCoordinate> getResolvedPluginArtifacts() {
        return List.copyOf(resolvedPluginArtifacts.values());
    }

    private void rememberResolvedPlugin(MavenCoordinate coord) {
        if (coord == null) {
            return;
        }

        String groupId = coord.getGroupId();
        String artifactId = coord.getArtifactId();
        if (groupId == null || artifactId == null) {
            return;
        }

        String version = coord.getVersion();
        String key = groupId + ":" + artifactId + ":" + (version != null ? version : "");
        resolvedPluginArtifacts.putIfAbsent(key, coord);
    }
}
