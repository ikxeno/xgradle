/*
 * Copyright 2026 BaseALT Ltd
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

import com.google.inject.Injector;

import org.altlinux.xgradle.impl.model.MavenCoordinate;

import unittests.Fixtures;
import unittests.metadata.Installations;

import org.gradle.api.invocation.Gradle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("FindMissingTransitivesStep")
class FindMissingTransitivesStepTests {

    @Test
    @DisplayName("reports missing dependencies at any depth and ignores optional ones")
    void reportsMissingTransitives(@TempDir Path metadata) {
        Fixtures.copy("missing-transitives", metadata);
        Injector injector = Installations.injector(List.of(metadata));

        ResolutionContext ctx = new ResolutionContext(mock(Gradle.class));
        ctx.putSystemArtifact("g:app", MavenCoordinate.builder().groupId("g").artifactId("app").version("1").build());
        injector.getInstance(FindMissingTransitivesStep.class).execute(ctx);

        assertEquals(Set.of("g:absent:1 (required by g:app)", "g:gone:1 (required by g:lib)"), ctx.getSkipped());
    }
}
