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
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.altlinux.xgradle.impl.resolution;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultResolutionReporter")
class DefaultResolutionReporterTests {

    @Mock
    private Gradle gradle;

    @Mock
    private Project root;

    @Mock
    private Logger logger;

    @Mock
    private TaskExecutionGraph graph;

    @Test
    @DisplayName("Does not register task graph when substitutions empty")
    void skipsTaskGraphWhenEmpty() {
        DefaultResolutionReporter reporter = new DefaultResolutionReporter();

        when(gradle.getRootProject()).thenReturn(root);
        when(root.getLogger()).thenReturn(logger);

        ResolutionContext ctx = new ResolutionContext(gradle);
        reporter.report(ctx);

        verify(graph, never()).whenReady(any(Action.class));
    }

    @Test
    @DisplayName("Registers task graph when substitutions present")
    void registersTaskGraphWhenNeeded() {
        DefaultResolutionReporter reporter = new DefaultResolutionReporter();

        when(gradle.getRootProject()).thenReturn(root);
        when(root.getLogger()).thenReturn(logger);
        when(gradle.getTaskGraph()).thenReturn(graph);

        ResolutionContext ctx = new ResolutionContext(gradle);
        ctx.getOverrideLogs().put("k", "v");

        reporter.report(ctx);

        verify(graph).whenReady(any(Action.class));
    }
}
