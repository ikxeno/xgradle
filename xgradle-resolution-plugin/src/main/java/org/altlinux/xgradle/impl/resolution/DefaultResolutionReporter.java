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

import org.altlinux.xgradle.interfaces.resolution.ResolutionReporter;
import org.altlinux.xgradle.impl.utils.logging.DependencyLogger;

import org.gradle.api.logging.Logger;

/**
 * Reports resolution results for the current build.
 * Implements {@link ResolutionReporter}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultResolutionReporter implements ResolutionReporter {


    @Inject
    DefaultResolutionReporter() {
    }

    @Override
    public void report(ResolutionContext resolutionContext) {
        Logger logger = resolutionContext.getGradle().getRootProject().getLogger();
        DependencyLogger depLogger = new DependencyLogger();

        depLogger.logSection("\n===== APPLYING SYSTEM DEPENDENCY VERSIONS =====", logger);
        depLogger.logSection("Initial dependencies", logger);
        depLogger.logInitialDependencies(resolutionContext.getProjectDependencies(), logger);

        depLogger.logSection("Resolved system artifacts", logger);
        depLogger.logResolvedArtifacts(resolutionContext.getSystemArtifacts(), logger);


        depLogger.logSection("===== DEPENDENCY RESOLUTION COMPLETED =====", logger);

        if (!resolutionContext.getNotFound().isEmpty() || !resolutionContext.getSkipped().isEmpty()) {
            depLogger.logSection("Skipped dependencies", logger);
            depLogger.logSkippedDependencies(
                    resolutionContext.getNotFound(),
                    resolutionContext.getSkipped(),
                    logger
            );
        }

        if (!resolutionContext.getOverrideLogs().isEmpty()) {
            resolutionContext.getGradle().getTaskGraph().whenReady(taskGraph -> {
                depLogger.logSection("Dependency substitutions", logger);
                depLogger.logSubstitutions(
                        resolutionContext.getOverrideLogs(),
                        logger
                );
            });
        }
    }
}
