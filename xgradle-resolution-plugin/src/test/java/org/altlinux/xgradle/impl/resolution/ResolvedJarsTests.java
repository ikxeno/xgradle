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

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.testfixtures.ProjectBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("ResolvedJars")
class ResolvedJarsTests {

    @Test
    @DisplayName("collects the jars of a configuration created after the watch starts, and only jars")
    void collectsJarsOfLaterConfigurations(@TempDir Path tempDir) throws IOException {
        Path libs = Files.createDirectories(tempDir.resolve("libs"));
        Files.writeString(libs.resolve("demo-1.jar"), "jar");
        Files.writeString(libs.resolve("notes-1.txt"), "txt");
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.resolve("project").toFile()).build();
        project.getRepositories().flatDir(repo -> repo.dirs(libs.toFile()));
        ResolvedJars resolvedJars = new ResolvedJars(mock(Logger.class));

        resolvedJars.watch(project.getConfigurations());
        Configuration later = project.getConfigurations().create("later");
        project.getDependencies().add("later", "g:demo:1");
        project.getDependencies().add("later", Map.of("group", "g", "name", "notes", "version", "1", "ext", "txt"));
        later.resolve();

        assertEquals(Set.of("demo-1.jar"),
                resolvedJars.jars().stream().map(File::getName).collect(Collectors.toSet()));
    }
}
