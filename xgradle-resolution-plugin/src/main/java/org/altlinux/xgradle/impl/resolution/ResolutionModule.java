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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.altlinux.xgradle.impl.collectors.DefaultSbomComponentCollector;
import org.altlinux.xgradle.impl.services.DefaultSbomGenerationService;
import org.altlinux.xgradle.interfaces.collectors.SbomComponentCollector;
import org.altlinux.xgradle.interfaces.resolution.ResolutionReporter;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.altlinux.xgradle.interfaces.resolution.SystemDependencyResolution;
import org.altlinux.xgradle.interfaces.services.SbomGenerationService;
/**
 * Guice module for Resolution bindings.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class ResolutionModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SystemDependencyResolution.class).to(DefaultSystemDependencyResolution.class);
        bind(ResolutionReporter.class).to(DefaultResolutionReporter.class);
        bind(SbomComponentCollector.class).to(DefaultSbomComponentCollector.class);
        bind(SbomGenerationService.class).to(DefaultSbomGenerationService.class);
        bindResolutionSteps();
    }

    private void bindResolutionSteps() {
        Multibinder<ResolutionStep> steps =
                Multibinder.newSetBinder(binder(), ResolutionStep.class);

        steps.addBinding().to(CollectDeclaredDependenciesStep.class);
        steps.addBinding().to(ResolveSystemArtifactsStep.class);
        steps.addBinding().to(FindMissingTransitivesStep.class);
        steps.addBinding().to(RecordOverridesStep.class);
        steps.addBinding().to(GenerateSbomStep.class);
    }
}
