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
import org.altlinux.xgradle.interfaces.resolvers.DependencySubstitutor;

/**
 * Configures dependency substitution based on requested versions, resolved system artifacts, and BOM-managed versions.
 * Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
@Order(1000)
final class ApplySubstitutionStep implements ResolutionStep {

    private final DependencySubstitutor substitutor;

    @Inject
    ApplySubstitutionStep(DependencySubstitutor substitutor) {
        this.substitutor = substitutor;
    }

    @Override
    public String name() {
        return "apply-substitution";
    }

    @Override
    public void execute(ResolutionContext resolutionContext) {
        resolutionContext.getOverrideLogs().clear();

        substitutor.configure(
                resolutionContext.getGradle(),
                resolutionContext.getRequestedVersions(),
                resolutionContext.getOverrideLogs()
        );
    }
}
