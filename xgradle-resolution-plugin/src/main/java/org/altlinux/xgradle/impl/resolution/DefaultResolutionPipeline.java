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
import org.altlinux.xgradle.interfaces.resolution.ResolutionPipeline;
import org.altlinux.xgradle.interfaces.resolution.Order;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * Pipeline for Resolution.
 * Implements {@link ResolutionPipeline}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

@Singleton
final class DefaultResolutionPipeline implements ResolutionPipeline {

    private final List<ResolutionStep> resolutionSteps;

    @Inject
    DefaultResolutionPipeline (Set<ResolutionStep> resolutionSteps) {
        this.resolutionSteps = orderSteps(resolutionSteps);
    }

    @Override
    public ResolutionContext run(ResolutionContext ext) {
        resolutionSteps.forEach(step -> step.execute(ext));
        return ext;
    }

    private static List<ResolutionStep> orderSteps(Set<ResolutionStep> steps) {
        return steps.stream()
                .sorted(Comparator.comparingInt(DefaultResolutionPipeline::order))
                .collect(Collectors.toUnmodifiableList());
    }

    private static int order(ResolutionStep step) {
        return Optional.ofNullable(step.getClass().getAnnotation(Order.class))
                .map(Order::value)
                .orElse(Order.LAST);
    }
}
