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
package org.altlinux.xgradle.impl.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.interfaces.collectors.SbomComponentCollector;
import org.altlinux.xgradle.interfaces.generators.SbomGenerator;
import org.altlinux.xgradle.interfaces.services.SbomGenerationService;

import org.gradle.api.Project;
import org.gradle.api.GradleException;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Generates an SBOM report from snapshots captured during resolution.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
public final class DefaultSbomGenerationService implements SbomGenerationService {

    private final SbomGenerator sbomGenerator;
    private final SbomComponentCollector sbomComponentCollector;

    @Inject
    public DefaultSbomGenerationService(
            SbomGenerator sbomGenerator,
            SbomComponentCollector sbomComponentCollector
    ) {
        this.sbomGenerator = sbomGenerator;
        this.sbomComponentCollector = sbomComponentCollector;
    }

    @Override
    public void generate(
            Gradle gradle,
            SbomFormat format,
            Map<String, MavenCoordinate> artifactsSnapshot,
            Collection<MavenCoordinate> pluginArtifactsSnapshot,
            Logger logger
    ) {
        try {
            Project root = gradle.getRootProject();
            List<SbomComponent> components = sbomComponentCollector.collect(
                    root,
                    artifactsSnapshot.values(),
                    pluginArtifactsSnapshot
            );

            Path outputPath = resolveOutputPath(root, format);
            sbomGenerator.generate(
                    format,
                    outputPath,
                    root.getName(),
                    String.valueOf(root.getVersion()),
                    components
            );

            logger.lifecycle("Generated {} SBOM: {}", format.name().toLowerCase(), outputPath);
        } catch (RuntimeException e) {
            throw new GradleException("Failed to generate the requested " + format + " SBOM", e);
        }
    }

    private Path resolveOutputPath(Project root, SbomFormat format) {
        return root.getLayout()
                .getBuildDirectory()
                .getAsFile()
                .get()
                .toPath()
                .resolve("reports")
                .resolve("xgradle")
                .resolve("sbom-" + format.getFileSuffix() + ".json");
    }
}
