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
package unittests.logging;

import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.utils.logging.DependencyLogger;
import org.gradle.api.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DependencyLogger contract")
class DependencyLoggerTests {

    @Mock
    private Logger logger;

    private final DependencyLogger dependencyLogger = new DependencyLogger();

    @Test
    @DisplayName("logSection prints formatted section title")
    void logSectionPrintsTitle() {
        dependencyLogger.logSection("Dependencies", logger);

        verify(logger).lifecycle(contains("Dependencies"));
    }

    @Test
    @DisplayName("logInitialDependencies logs dependency count")
    void logInitialDependenciesLogsCount() {
        dependencyLogger.logInitialDependencies(Set.of("g:a", "g:b"), logger);

        verify(logger).lifecycle("Found {} dependencies", 2);
    }

    @Test
    @DisplayName("logResolvedArtifacts logs artifact count and each artifact")
    void logResolvedArtifactsLogsCountAndDetails() {
        MavenCoordinate coord = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("a")
                .version("1.0")
                .build();

        dependencyLogger.logResolvedArtifacts(Map.of("g:a", coord), logger);

        verify(logger).lifecycle("Resolved {} artifacts", 1);
        verify(logger).info(eq(" - {}:{}:{}"), eq("g"), eq("a"), eq("1.0"));
    }

    @Test
    @DisplayName("logNewDependencies logs count and each dependency")
    void logNewDependenciesLogsCountAndEach() {
        dependencyLogger.logNewDependencies(Set.of("g:a"), logger);

        verify(logger).lifecycle("Found {} dependencies", 1);
        verify(logger).info(" - {}", "g:a");
    }

    @Test
    @DisplayName("logTestContextDependencies logs count and each test dep")
    void logTestContextDependenciesLogsCountAndEach() {
        dependencyLogger.logTestContextDependencies(Set.of("g:test"), logger);

        verify(logger).lifecycle("Test context dependencies: {}", 1);
        verify(logger).info(" - {}", "g:test");
    }

    @Test
    @DisplayName("logConfigurationArtifacts logs each configuration with its artifacts")
    void logConfigurationArtifactsLogsEach() {
        dependencyLogger.logConfigurationArtifacts(
                Map.of("runtimeClasspath", Set.of("g:a:1.0")),
                logger
        );

        verify(logger).lifecycle(any(), contains("runtimeClasspath"), eq(1));
        verify(logger).lifecycle(" - {}", "g:a:1.0");
    }

    @Test
    @DisplayName("logSkippedDependencies skips both logs when collections are empty")
    void logSkippedDependenciesSkipsWhenEmpty() {
        dependencyLogger.logSkippedDependencies(Set.of(), Set.of(), logger);

        verifyNoMoreInteractions(logger);
    }

    @Test
    @DisplayName("logSkippedDependencies logs only notFound when skipped is empty")
    void logSkippedDependenciesLogsNotFoundOnly() {
        dependencyLogger.logSkippedDependencies(Set.of("g:missing"), Set.of(), logger);

        verify(logger).lifecycle(contains("Not installed dependencies"));
        verify(logger).lifecycle(" - {}", "g:missing");
        verifyNoMoreInteractions(logger);
    }

    @Test
    @DisplayName("logSkippedDependencies logs only skipped when notFound is empty")
    void logSkippedDependenciesLogsSkippedOnly() {
        dependencyLogger.logSkippedDependencies(Set.of(), Set.of("g:transitive"), logger);

        verify(logger).lifecycle(contains("Not found transitive"));
        verify(logger).lifecycle(" - {}", "g:transitive");
        verifyNoMoreInteractions(logger);
    }

    @Test
    @DisplayName("logSubstitutions skips both logs when maps are empty")
    void logSubstitutionsSkipsWhenEmpty() {
        dependencyLogger.logSubstitutions(Map.of(), Map.of(), logger);

        verifyNoInteractions(logger);
    }

    @Test
    @DisplayName("logSubstitutions logs only overrides when applyLogs is empty")
    void logSubstitutionsLogsOverridesOnly() {
        dependencyLogger.logSubstitutions(Map.of("g:a", "g:a:1.0 -> 2.0"), Map.of(), logger);

        verify(logger).lifecycle(contains("Overridden"));
        verify(logger).lifecycle("g:a:1.0 -> 2.0");
        verifyNoMoreInteractions(logger);
    }

    @Test
    @DisplayName("logSubstitutions logs only applied versions when overrideLogs is empty")
    void logSubstitutionsLogsAppliedOnly() {
        dependencyLogger.logSubstitutions(Map.of(), Map.of("g:b", "g:b:3.0"), logger);

        verify(logger).info(contains("Applied"));
        verify(logger).info("g:b:3.0");
        verifyNoMoreInteractions(logger);
    }
}
