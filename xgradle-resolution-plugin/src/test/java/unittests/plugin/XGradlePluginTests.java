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
package unittests.plugin;

import org.altlinux.xgradle.impl.plugin.XGradlePlugin;
import org.gradle.StartParameter;
import org.gradle.api.Action;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.GradleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("XGradlePlugin contract")
class XGradlePluginTests {

    private final XGradlePlugin plugin = new XGradlePlugin();

    @AfterEach
    void tearDown() {
        System.clearProperty("disable.xgradle");
        System.clearProperty("maven.metadata.dir");
    }

    @Test
    @DisplayName("apply returns immediately when disable.xgradle=true")
    void applyReturnsImmediatelyWhenDisabled() {
        System.setProperty("disable.xgradle", "true");
        Gradle gradle = mock(Gradle.class);

        plugin.apply(gradle);

        verifyNoInteractions(gradle);
    }

    @Test
    @DisplayName("apply registers Gradle hooks when disable.xgradle is not set")
    void applyRegistersHooksWhenEnabled() {
        StartParameter startParameter = mock(StartParameter.class);
        Gradle gradle = mock(Gradle.class);
        when(gradle.getStartParameter()).thenReturn(startParameter);
        when(startParameter.getCurrentDir()).thenReturn(new File(System.getProperty("java.io.tmpdir")));

        plugin.apply(gradle);

        verify(gradle).beforeSettings(any(Action.class));
        verify(gradle).projectsEvaluated(any(Action.class));
    }

    @Test
    @DisplayName("apply registers Gradle hooks when disable.xgradle=false")
    void applyRegistersHooksWhenDisableFlagIsFalse() {
        System.setProperty("disable.xgradle", "false");
        StartParameter startParameter = mock(StartParameter.class);
        Gradle gradle = mock(Gradle.class);
        when(gradle.getStartParameter()).thenReturn(startParameter);
        when(startParameter.getCurrentDir()).thenReturn(new File(System.getProperty("java.io.tmpdir")));

        plugin.apply(gradle);

        verify(gradle).beforeSettings(any(Action.class));
        verify(gradle).projectsEvaluated(any(Action.class));
    }

    @Test
    @DisplayName("apply fails with the GradleException itself, not wrapped by Guice")
    void applyReportsMissingMetadataLocationDirectly(@TempDir File tempDir) {
        System.setProperty("maven.metadata.dir", new File(tempDir, "missing").getPath());
        StartParameter startParameter = mock(StartParameter.class);
        Gradle gradle = mock(Gradle.class);
        when(gradle.getStartParameter()).thenReturn(startParameter);
        when(startParameter.getCurrentDir()).thenReturn(tempDir);

        GradleException e = assertThrows(GradleException.class, () -> plugin.apply(gradle));

        assertTrue(e.getMessage().contains("missing"), e.getMessage());
    }
}
