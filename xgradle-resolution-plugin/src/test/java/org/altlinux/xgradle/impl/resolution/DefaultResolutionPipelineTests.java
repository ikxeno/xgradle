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

import org.altlinux.xgradle.interfaces.resolution.Order;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.gradle.api.invocation.Gradle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultResolutionPipeline")
class DefaultResolutionPipelineTests {

    @Mock
    private ResolutionStep step1;

    @Mock
    private ResolutionStep step2;

    @Mock
    private Gradle gradle;

    @Test
    @DisplayName("Executes steps in order")
    void executesStepsInOrder() {
        DefaultResolutionPipeline pipeline = new DefaultResolutionPipeline(
                new LinkedHashSet<>(List.of(step1, step2))
        );
        ResolutionContext context = new ResolutionContext(gradle);

        pipeline.run(context);

        InOrder order = inOrder(step1, step2);
        order.verify(step1).execute(context);
        order.verify(step2).execute(context);
    }

    @Test
    @DisplayName("Runs steps by their @Order, unannotated ones last")
    void runsStepsByOrder() {
        List<String> ran = new ArrayList<>();
        DefaultResolutionPipeline pipeline = new DefaultResolutionPipeline(new LinkedHashSet<>(List.of(
                new Unordered(ran), new Late(ran), new Early(ran))));

        pipeline.run(new ResolutionContext(gradle));

        assertEquals(List.of("early", "late", "unordered"), ran);
    }

    private abstract static class RecordingStep implements ResolutionStep {

        private final List<String> ran;
        private final String name;

        RecordingStep(List<String> ran, String name) {
            this.ran = ran;
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void execute(ResolutionContext ctx) {
            ran.add(name);
        }
    }

    @Order(10)
    private static final class Early extends RecordingStep {
        Early(List<String> ran) {
            super(ran, "early");
        }
    }

    @Order(20)
    private static final class Late extends RecordingStep {
        Late(List<String> ran) {
            super(ran, "late");
        }
    }

    private static final class Unordered extends RecordingStep {
        Unordered(List<String> ran) {
            super(ran, "unordered");
        }
    }
}
