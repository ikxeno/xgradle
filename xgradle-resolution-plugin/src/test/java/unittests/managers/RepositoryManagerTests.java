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
package unittests.managers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.impl.managers.ManagersModule;
import org.altlinux.xgradle.impl.model.IvyRepository;
import org.altlinux.xgradle.interfaces.managers.RepositoryManager;
import org.altlinux.xgradle.interfaces.metadata.SystemRepository;
import org.altlinux.xgradle.interfaces.processors.PluginProcessor;
import org.altlinux.xgradle.interfaces.resolvers.DependencySubstitutor;

import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.plugin.management.PluginManagementSpec;
import org.gradle.testfixtures.ProjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("RepositoryManager contract")
class RepositoryManagerTests {

    @TempDir
    Path temp;

    private RepositoryManager manager;
    private RepositoryHandler repositories;
    private IvyRepository repository;

    @BeforeEach
    void setUp() {
        manager = Guice.createInjector(Modules.override(new ManagersModule()).with(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Logger.class).toInstance(mock(Logger.class));
                bind(PluginProcessor.class).toInstance(mock(PluginProcessor.class));
                bind(DependencySubstitutor.class).toInstance(mock(DependencySubstitutor.class));
                bind(SystemRepository.class).toInstance(mock(SystemRepository.class));
            }
        })).getInstance(RepositoryManager.class);

        Project project = ProjectBuilder.builder().withProjectDir(temp.resolve("project").toFile()).build();
        repositories = project.getRepositories();
        repositories.mavenCentral();
        repository = new IvyRepository(temp.resolve("repo"));
    }

    @Test
    @DisplayName("configureDependenciesRepository puts the system ivy repository first")
    void dependenciesRepositoryFirst() {
        manager.configureDependenciesRepository(repositories, repository);

        assertEquals(List.of("SystemDepsRepo", "MavenRepo"), names());
        IvyArtifactRepository ivy = (IvyArtifactRepository) repositories.get(0);
        assertEquals(repository.getRoot().toUri(), ivy.getUrl());
    }

    @Test
    @DisplayName("configurePluginsRepository puts the system ivy repository first")
    void pluginsRepositoryFirst() {
        Settings settings = mock(Settings.class);
        PluginManagementSpec pluginManagement = mock(PluginManagementSpec.class);
        when(settings.getPluginManagement()).thenReturn(pluginManagement);
        when(pluginManagement.getRepositories()).thenReturn(repositories);

        manager.configurePluginsRepository(settings, repository);

        assertEquals(List.of("SystemPluginsRepo", "MavenRepo"), names());
    }

    @Test
    @DisplayName("configureProjectRepository adds the repository first only once the project has repositories")
    void projectRepositoryFollowsProjectRepositories() {
        RepositoryHandler empty = ProjectBuilder.builder().withProjectDir(temp.resolve("empty").toFile()).build()
                .getRepositories();

        manager.configureProjectRepository(empty, repository);
        assertTrue(empty.isEmpty(), "a project without repositories keeps the settings repositories");

        empty.mavenCentral();
        empty.google();
        assertEquals(List.of("SystemDepsRepo", "MavenRepo", "Google"),
                empty.stream().map(ArtifactRepository::getName).collect(Collectors.toList()));
    }

    private List<String> names() {
        return repositories.stream().map(ArtifactRepository::getName).collect(Collectors.toList());
    }
}
