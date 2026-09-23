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

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.invocation.Gradle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResolvedJars")
class ResolvedJarsTests {

    @Mock
    private Gradle gradle;

    @Mock
    private Project rootProject;

    @Mock
    private Project project;

    @Mock
    private ConfigurationContainer configurationContainer;

    @Mock
    private Configuration configuration;

    @Mock
    private ResolvableDependencies incoming;

    @Mock
    private ResolvedConfiguration resolvedConfiguration;

    @Mock
    private ResolvedArtifact jarArtifact;

    @Mock
    private ResolvedArtifact textArtifact;


    @Test
    @DisplayName("Collects only .jar files resolved by any resolvable configuration")
    void collectsOnlyJarFiles(@TempDir Path tempDir) throws Exception {
        when(gradle.getRootProject()).thenReturn(rootProject);
        when(rootProject.getAllprojects()).thenReturn(Set.of(project));

        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(configurationContainer.stream()).thenReturn(Stream.of(configuration));
        when(configuration.isCanBeResolved()).thenReturn(true);
        when(configuration.getIncoming()).thenReturn(incoming);
        when(configuration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);

        Path jar = Files.createFile(tempDir.resolve("demo.jar"));
        Path txt = Files.createFile(tempDir.resolve("notes.txt"));
        when(jarArtifact.getFile()).thenReturn(jar.toFile());
        when(textArtifact.getFile()).thenReturn(txt.toFile());
        when(resolvedConfiguration.getResolvedArtifacts()).thenReturn(Set.of(jarArtifact, textArtifact));

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Action<ResolvableDependencies> action = invocation.getArgument(0);
            action.execute(incoming);
            return null;
        }).when(incoming).afterResolve(
                org.mockito.ArgumentMatchers.<Action<? super ResolvableDependencies>>any()
        );

        Set<File> resolved = ResolvedJars.watch(gradle);

        assertAll(
                () -> assertTrue(resolved.contains(jar.toFile())),
                () -> assertFalse(resolved.contains(txt.toFile()))
        );
    }
}
