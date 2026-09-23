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
import com.google.inject.name.Named;

import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.interfaces.maven.PomHierarchyLoader;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
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

/**
 * Loads a hierarchy of Maven POM models starting from a specified POM file.
 * Implements {@link PomHierarchyLoader}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class MavenPomHierarchyLoader implements PomHierarchyLoader {
    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();
    private final DefaultModelReader modelReader = new DefaultModelReader();

    private final MetadataIndex index;
    private final Logger logger;

    /**
     * @param index artifacts described by XMvn metadata; parents of POMs installed
     *              without metadata are found next to them
     */
    @Inject
    MavenPomHierarchyLoader(@Named(MetadataIndex.XMVN_METADATA) MetadataIndex index, Logger logger) {
        this.index = index;
        this.logger = logger;
    }

    @Override
    public List<Model> loadHierarchy(Path pomPath) {
        Deque<Model> stack = new ArrayDeque<>();
        Path currentPath = pomPath;
        int depth = 0;
        final int MAX_DEPTH = 10;

        while (currentPath != null && depth < MAX_DEPTH) {
            Model model = loadModel(currentPath);
            stack.push(model);
            Parent parent = model.getParent();
            if (parent == null) {
                break;
            }

            currentPath = resolveParentPath(currentPath, parent).orElse(null);
            if (currentPath == null) {
                logger.warn("Parent POM {}:{}:{} of {} is not installed; inherited properties and managed versions are missing",
                        parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), pomPath);
            }
            depth++;
        }
        return new ArrayList<>(stack);
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
     * metadata finds its parent in a sibling file named after the parent artifactId.
     */
    private Optional<Path> resolveParentPath(Path childPath, Parent parent) {
        ArtifactKey key = new ArtifactKey(
                parent.getGroupId(), parent.getArtifactId(), ArtifactKey.POM_EXTENSION, "", parent.getVersion());
        return index.resolve(key)
                .map(XmvnArtifact::getPath)
                .or(() -> Optional.of(childPath.resolveSibling(parent.getArtifactId() + ".pom"))
                        .filter(Files::isRegularFile));
    }
}
