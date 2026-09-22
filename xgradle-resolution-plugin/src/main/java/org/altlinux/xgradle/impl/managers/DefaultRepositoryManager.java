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
package org.altlinux.xgradle.impl.managers;

import com.google.inject.Inject;
import org.altlinux.xgradle.impl.utils.config.XGradleConfig;
import org.altlinux.xgradle.interfaces.managers.RepositoryManager;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the addition of system-level dependency repositories to a Gradle build.
 * Implements {@link RepositoryManager}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class DefaultRepositoryManager implements RepositoryManager {

    private static final String SCAN_DEPTH_KEY = "xgradle.scan.depth";
    private static final int DEFAULT_SCAN_DEPTH = 3;
    private static final String DEPENDENCIES_REPO_NAME = "SystemDepsRepo";

    private final Logger logger;

    @Inject
    DefaultRepositoryManager(Logger logger) {
        this.logger = logger;
    }

    public void configurePluginsRepository(Settings settings, List<File> baseDirs) {
        List<File> validDirs = getValidDirectories(baseDirs);
        if (validDirs.isEmpty()) {
            logger.warn("No valid system jars directories for plugin repositories");
            return;
        }
        List<File> dirs = scanDirectories(validDirs);
        settings.getPluginManagement().getRepositories().flatDir(repo -> {
            repo.setName("SystemPluginsRepo");
            dirs.forEach(repo::dir);
            logger.info("Configured PluginManagement repository with {} directories", dirs.size());
        });
    }

    public void configureDependenciesRepository(RepositoryHandler repos, List<File> baseDirs) {
        List<File> validDirs = requireValidDirectories(baseDirs);

        FlatDirectoryArtifactRepository flatRepo =
                createFlatRepository(repos, DEPENDENCIES_REPO_NAME, validDirs);

        repos.remove(flatRepo);
        repos.addFirst(flatRepo);
    }

    private List<File> scanDirectories(List<File> baseDirs) {
        LinkedHashSet<File> allDirs = new LinkedHashSet<>();
        int scanDepth = XGradleConfig.getIntProperty(SCAN_DEPTH_KEY, DEFAULT_SCAN_DEPTH);
        baseDirs.forEach(baseDir -> {
            File root = baseDir.getAbsoluteFile();
            Path basePath = root.toPath();
            allDirs.add(root);
            try (Stream<Path> pathStream = Files.walk(basePath, scanDepth)) {
                pathStream.filter(Files::isDirectory)
                        .filter(path -> !path.equals(basePath))
                        .forEach(path -> allDirs.add(path.toFile()));
            } catch (Exception e) {
                logger.error("Directory scan error: {}", e.getMessage());
            }
        });
        return new ArrayList<>(allDirs);
    }

    private List<File> getValidDirectories(List<File> baseDirs) {
        if (baseDirs == null || baseDirs.isEmpty()) {
            return List.of();
        }
        List<File> validDirs = baseDirs.stream()
                .filter(dir -> dir != null && dir.isDirectory() && dir.canRead())
                .collect(Collectors.toList());
        List<File> invalidDirs = baseDirs.stream()
                .filter(dir -> dir == null || !dir.isDirectory() || !dir.canRead())
                .collect(Collectors.toList());
        if (!invalidDirs.isEmpty()) {
            logger.warn("Skipping invalid lib directories: {}", invalidDirs);
        }
        return validDirs;
    }

    private List<File> requireValidDirectories(List<File> baseDirs) {
        List<File> validDirs = getValidDirectories(baseDirs);
        if (!validDirs.isEmpty()) {
            return validDirs;
        }
        if (baseDirs == null || baseDirs.isEmpty()) {
            throw new GradleException("java.library.dir is not set or empty");
        }
        throw new GradleException("No valid lib directories found in java.library.dir: " + baseDirs);
    }

    private FlatDirectoryArtifactRepository createFlatRepository(
            RepositoryHandler repos,
            String repoName,
            List<File> baseDirs
    ) {
        return repos.flatDir(repo -> {
            repo.setName(repoName);
            List<File> allDirs = scanDirectories(baseDirs);
            allDirs.forEach(repo::dir);
            logger.info("Configured DependencyManagement repository with {} directories", allDirs.size());
        });
    }
}
