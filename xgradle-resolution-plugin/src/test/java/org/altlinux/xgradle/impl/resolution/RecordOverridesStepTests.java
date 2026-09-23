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

import org.altlinux.xgradle.interfaces.resolvers.DependencySubstitutor;

import org.gradle.api.invocation.Gradle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("RecordOverridesStep")
class RecordOverridesStepTests {

    @Test
    @DisplayName("records each declared version that resolves to another installed revision")
    void recordsReplacedVersions() {
        DependencySubstitutor substitutor = mock(DependencySubstitutor.class);
        when(substitutor.replacement(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(substitutor.replacement("g", "lib", "1.0")).thenReturn(Optional.of("1.5"));
        ResolutionContext ctx = new ResolutionContext(mock(Gradle.class));
        ctx.getRequestedVersions().put("g:lib", Set.of("1.0", "1.5"));

        new RecordOverridesStep(substitutor).execute(ctx);

        assertEquals(Map.of("g:lib|1.0|1.5", "Override version: g:lib:1.0 -> 1.5"), ctx.getOverrideLogs());
    }
}
