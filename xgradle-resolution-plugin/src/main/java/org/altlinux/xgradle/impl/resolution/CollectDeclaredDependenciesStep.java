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

import org.altlinux.xgradle.interfaces.collectors.DependencyCollector;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.altlinux.xgradle.interfaces.resolution.Order;

/**
 * Collects declared project dependencies and requested versions.
 * Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
@Order(400)
final class CollectDeclaredDependenciesStep implements ResolutionStep {

    private final DependencyCollector dependencyCollector;

    @Inject
    CollectDeclaredDependenciesStep(DependencyCollector dependencyCollector) {
        this.dependencyCollector = dependencyCollector;
    }

    @Override
    public String name() {
        return "collect-declared-dependencies";
    }

    @Override
    public void execute(ResolutionContext resolutionContext) {
        resolutionContext.getProjectDependencies().clear();
        resolutionContext.getProjectDependencies().addAll(
                dependencyCollector.collect(resolutionContext.getGradle())
        );

        resolutionContext.getRequestedVersions().clear();
        resolutionContext.getRequestedVersions().putAll(
                dependencyCollector.getRequestedVersions()
        );
    }
}
