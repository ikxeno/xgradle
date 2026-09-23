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
import com.google.inject.name.Named;

import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.InstalledLayout;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.metadata.PomArtifactReader;

import org.gradle.api.logging.Logger;

import java.nio.file.Path;
import java.util.List;
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
            @Named(MetadataIndex.XMVN_METADATA) MetadataIndex xmvnIndex,
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
                .map(InstalledIndexProvider::module)
                .collect(Collectors.toSet());

        List<XmvnArtifact> fromPoms = pomReader.read(layout.getPomsRoot(), layout.getJavaRoot(), describedFiles).stream()
                .filter(artifact -> !describedModules.contains(artifact.getGroupId() + ":" + artifact.getArtifactId()))
                .collect(Collectors.toList());

        DefaultMetadataIndex index = new DefaultMetadataIndex(
                Stream.concat(fromMetadata.stream(), fromPoms.stream()).collect(Collectors.toList()),
                layout.isIgnoreDuplicateMetadata());
        index.conflicts().forEach(logger::warn);
        logger.info("Indexed {} installed artifacts", index.artifacts().size());
        return index;
    }

    private static String module(ArtifactKey key) {
        return key.getGroupId() + ":" + key.getArtifactId();
    }
}
