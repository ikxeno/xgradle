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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.utils.config.XGradleConfig;
import org.altlinux.xgradle.interfaces.processors.PluginProcessor;
import org.altlinux.xgradle.interfaces.resolution.Order;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.altlinux.xgradle.interfaces.services.SbomGenerationService;
import org.altlinux.xgradle.impl.enums.SbomFormat;

import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates an SBOM report after dependency resolution when enabled by configuration.
 * Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
@Order(1200)
final class GenerateSbomStep implements ResolutionStep {

    private static final String BUILD_END_SERVICE = "xgradleSbomAtBuildEnd";
    private static final String GENERATE_SBOM_KEY = "generate.sbom";

    private final SbomGenerationService sbomGenerationService;
    private final PluginProcessor pluginProcessor;

    @Inject
    GenerateSbomStep(
            SbomGenerationService sbomGenerationService,
            PluginProcessor pluginProcessor
    ) {
        this.sbomGenerationService = sbomGenerationService;
        this.pluginProcessor = pluginProcessor;
    }

    @Override
    public String name() {
        return "generate-sbom";
    }

    @Override
    public void execute(ResolutionContext resolutionContext) {
        String configuredFormat = XGradleConfig.getProperty(GENERATE_SBOM_KEY);
        if (configuredFormat == null || configuredFormat.isBlank()) {
            return;
        }

        Optional<SbomFormat> parsedFormat = SbomFormat.fromProperty(configuredFormat);
        Logger logger = resolutionContext.getGradle().getRootProject().getLogger();
        if (parsedFormat.isEmpty()) {
            logger.warn(
                    "Unsupported SBOM format '{}'. Allowed values: spdx, cyclonedx",
                    configuredFormat
            );
            return;
        }

        Gradle gradle = resolutionContext.getGradle();
        Map<String, MavenCoordinate> artifactsSnapshot =
                new LinkedHashMap<>(resolutionContext.getSystemArtifacts());
        SbomFormat sbomFormat = parsedFormat.get();
        Collection<MavenCoordinate> pluginArtifactsSnapshot =
                snapshotPluginArtifacts();

        gradle.getSharedServices()
                .registerIfAbsent(BUILD_END_SERVICE, BuildEndAction.class, spec -> { })
                .get()
                .setAction(() -> sbomGenerationService.generate(
                        gradle,
                        sbomFormat,
                        artifactsSnapshot,
                        pluginArtifactsSnapshot,
                        logger
                ));
    }

    private Collection<MavenCoordinate> snapshotPluginArtifacts() {
        Collection<MavenCoordinate> resolvedPlugins =
                pluginProcessor.getResolvedPluginArtifacts();
        if (resolvedPlugins == null || resolvedPlugins.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(resolvedPlugins);
    }
}
