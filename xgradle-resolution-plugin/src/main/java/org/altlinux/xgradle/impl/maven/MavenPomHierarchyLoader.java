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
package org.altlinux.xgradle.impl.maven;

import com.google.inject.Inject;

import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.interfaces.maven.PomHierarchyLoader;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.metadata.XmvnMetadataOnly;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.DefaultModelReader;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads a hierarchy of Maven POM models starting from a specified POM file.
 * Implements {@link PomHierarchyLoader}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class MavenPomHierarchyLoader implements PomHierarchyLoader {
    private static final int MAX_DEPTH = 10;

    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();
    private final DefaultModelReader modelReader = new DefaultModelReader();

    private final MetadataIndex index;
    private final Logger logger;

    /**
     * @param index artifacts described by XMvn metadata; parents of POMs installed
     *              without metadata are found next to them
     */
    @Inject
    MavenPomHierarchyLoader(@XmvnMetadataOnly MetadataIndex index, Logger logger) {
        this.index = index;
        this.logger = logger;
    }

    /**
     * The POM and its parents, the root parent first. A parent that is not installed
     * or cannot be read ends the chain with a warning; the POM itself must be readable.
     */
    @Override
    public List<Model> loadHierarchy(Path pomPath) {
        Deque<Model> stack = new ArrayDeque<>();
        Path currentPath = pomPath;
        Model model = loadModel(pomPath);
        stack.push(model);

        for (int depth = 0; depth < MAX_DEPTH && model.getParent() != null; depth++) {
            Parent parent = model.getParent();
            Optional<Path> parentPath = resolveParentPath(currentPath, parent);
            Optional<Model> parentModel = parentPath.flatMap(this::loadParent);
            if (parentModel.isEmpty()) {
                logger.warn("Parent POM {}:{}:{} of {} is not installed; "
                                + "inherited properties and managed versions are missing",
                        parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), pomPath);
                break;
            }
            currentPath = parentPath.get();
            model = parentModel.get();
            stack.push(model);
        }
        return new ArrayList<>(stack);
    }

    private Optional<Model> loadParent(Path pomPath) {
        try {
            return Optional.of(loadModel(pomPath));
        } catch (GradleException e) {
            logger.warn("Skipping unreadable parent POM {}: {}", pomPath, e.getMessage());
            return Optional.empty();
        }
    }

    private Model loadModel(Path pomPath) {
        return modelCache.computeIfAbsent(pomPath.toString(), cacheKey -> {
            try (InputStream inputStream = Files.newInputStream(pomPath)) {
                return modelReader.read(inputStream, null);
            } catch (IOException exception) {
                throw new GradleException("Cannot read POM " + pomPath + ": " + exception.getMessage(), exception);
            }
        });
    }

    /**
     * Path of the parent POM. As in XMvn, the metadata is searched for the parent's
     * version as a compat version first, then for the system version. A POM without
     * metadata finds its parent next to it.
     */
    private Optional<Path> resolveParentPath(Path childPath, Parent parent) {
        ArtifactKey key = new ArtifactKey(
                parent.getGroupId(), parent.getArtifactId(), ArtifactKey.POM_EXTENSION, "", parent.getVersion());
        return index.resolve(key)
                .map(XmvnArtifact::getPath)
                .filter(Files::isRegularFile)
                .or(() -> siblingParent(childPath, parent));
    }

    /**
     * A sibling POM of the parent module, named after its artifactId as xgradle-cli
     * installs it or with the JPP names of older packages ({@code JPP-a.pom},
     * {@code JPP.dir-a.pom}). A JPP name can be ambiguous, so the POM's own
     * coordinates must match.
     */
    private Optional<Path> siblingParent(Path childPath, Parent parent) {
        Path dir = childPath.toAbsolutePath().getParent();
        String artifactId = parent.getArtifactId();
        return Stream.concat(
                        Stream.of(dir.resolve(artifactId + ".pom"), dir.resolve("JPP-" + artifactId + ".pom")),
                        jppDirectoryNames(dir, artifactId))
                .filter(Files::isRegularFile)
                .filter(pom -> loadParent(pom).filter(model -> describes(model, parent)).isPresent())
                .findFirst();
    }

    private static Stream<Path> jppDirectoryNames(Path dir, String artifactId) {
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(file -> {
                        String name = file.getFileName().toString();
                        return name.startsWith("JPP.") && name.endsWith("-" + artifactId + ".pom");
                    })
                    .sorted()
                    .collect(Collectors.toList())
                    .stream();
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private static boolean describes(Model model, Parent parent) {
        String groupId = model.getGroupId() != null || model.getParent() == null
                ? model.getGroupId()
                : model.getParent().getGroupId();
        return parent.getGroupId().equals(groupId) && parent.getArtifactId().equals(model.getArtifactId());
    }
}
