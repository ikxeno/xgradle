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
package org.altlinux.xgradle.impl.metadata;

import com.google.inject.Inject;
import com.google.inject.Provider;

import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.InstalledLayout;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.metadata.XmvnMetadataOnly;
import org.altlinux.xgradle.interfaces.metadata.PomArtifactReader;

import org.gradle.api.logging.Logger;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds the index of everything installed. XMvn metadata is the primary
 * source. A POM installed without metadata, as xgradle-cli installs
 * Gradle-built packages, adds a module only when no metadata entry or alias
 * already provides its groupId:artifactId.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class InstalledIndexProvider implements Provider<MetadataIndex> {

    private final InstalledLayout layout;
    private final MetadataIndex xmvnIndex;
    private final PomArtifactReader pomReader;
    private final Logger logger;

    @Inject
    InstalledIndexProvider(
            InstalledLayout layout,
            @XmvnMetadataOnly MetadataIndex xmvnIndex,
            PomArtifactReader pomReader,
            Logger logger
    ) {
        this.layout = layout;
        this.xmvnIndex = xmvnIndex;
        this.pomReader = pomReader;
        this.logger = logger;
    }

    @Override
    public MetadataIndex get() {
        List<XmvnArtifact> fromMetadata = xmvnIndex.artifacts();
        Set<Path> describedFiles = fromMetadata.stream()
                .map(XmvnArtifact::getPath)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> describedModules = fromMetadata.stream()
                .flatMap(artifact -> artifact.lookupKeys().stream())
                .map(ArtifactKey::module)
                .collect(Collectors.toSet());

        List<XmvnArtifact> fromPoms = firstOfEachKey(
                pomReader.read(layout.getPomsRoot(), layout.getJavaRoots(), describedFiles).stream()
                        .filter(artifact -> !describedModules.contains(artifact.module())));

        DefaultMetadataIndex index = new DefaultMetadataIndex(
                Stream.concat(fromMetadata.stream(), fromPoms.stream()).collect(Collectors.toList()),
                layout.isIgnoreDuplicateMetadata());
        index.conflicts().forEach(logger::warn);
        logger.info("Indexed {} installed artifacts", index.artifacts().size());
        return index;
    }

    /**
     * XMvn has no rule for POMs it never sees, and dropping both claims would lose the
     * module, so of two POMs with the same coordinates the first in path order is kept.
     */
    private List<XmvnArtifact> firstOfEachKey(Stream<XmvnArtifact> artifacts) {
        Map<ArtifactKey, XmvnArtifact> first = new LinkedHashMap<>();
        artifacts.forEach(artifact -> {
            ArtifactKey key = artifact.lookupKeys().get(0);
            XmvnArtifact kept = first.putIfAbsent(key, artifact);
            if (kept != null) {
                logger.warn("Ignoring POM {}: {} is already installed by {}",
                        artifact.getMetadataFile(), key, kept.getMetadataFile());
            }
        });
        return List.copyOf(first.values());
    }
}
