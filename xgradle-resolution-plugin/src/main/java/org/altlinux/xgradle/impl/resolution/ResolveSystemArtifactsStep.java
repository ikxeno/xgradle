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

import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.altlinux.xgradle.interfaces.resolution.Order;
import org.altlinux.xgradle.interfaces.resolvers.ArtifactResolver;

/**
 * Resolves system artifacts for the current build based on collected dependencies.
 * Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
@Order(700)
final class ResolveSystemArtifactsStep implements ResolutionStep {

    private final ArtifactResolver artifactResolver;

    @Inject
    ResolveSystemArtifactsStep(ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
    }

    @Override
    public String name() {
        return "resolve-system-artifacts";
    }

    @Override
    public void execute(ResolutionContext resolutionContext) {
        artifactResolver.resolve(
                resolutionContext.getProjectDependencies(),
                resolutionContext.getGradle().getRootProject().getLogger()
        );
        artifactResolver.filter();

        resolutionContext.getSystemArtifacts().clear();
        resolutionContext.getSystemArtifacts().putAll(artifactResolver.getSystemArtifacts());

        resolutionContext.getNotFound().clear();
        resolutionContext.getNotFound().addAll(artifactResolver.getNotFoundDependencies());
    }
}
