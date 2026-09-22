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
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.metadata.MetadataReader;

import org.gradle.api.logging.Logger;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Artifact index with the semantics of XMvn's {@code DefaultMetadataResult}
 * (duplicates are kept, as with XMvn's default {@code ignoreDuplicateMetadata=false}).
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultMetadataIndex implements MetadataIndex {

    private final MetadataReader reader;
    private final Logger logger;

    private List<XmvnArtifact> artifacts = List.of();
    private Map<ArtifactKey, XmvnArtifact> byKey = Map.of();

    @Inject
    DefaultMetadataIndex(MetadataReader reader, Logger logger) {
        this.reader = reader;
        this.logger = logger;
    }

    @Override
    public void build(List<Path> locations) {
        List<XmvnArtifact> read = reader.read(locations);
        Map<ArtifactKey, XmvnArtifact> index = new LinkedHashMap<>();
        read.forEach(artifact -> artifact.lookupKeys().forEach(key -> put(index, key, artifact)));

        artifacts = List.copyOf(read);
        byKey = index;
        logger.info("Indexed {} artifacts from XMvn metadata in {}", artifacts.size(), locations);
    }

    /**
     * On a duplicate key the later artifact wins, unless only the earlier one
     * belongs to a namespace - the same rule XMvn applies.
     */
    private void put(Map<ArtifactKey, XmvnArtifact> index, ArtifactKey key, XmvnArtifact artifact) {
        XmvnArtifact existing = index.putIfAbsent(key, artifact);
        if (existing == null || existing == artifact) {
            return;
        }
        logger.warn("Duplicate XMvn metadata for {}: {} and {}",
                key, existing.getMetadataFile(), artifact.getMetadataFile());
        if (existing.getNamespace().isEmpty() || !artifact.getNamespace().isEmpty()) {
            index.put(key, artifact);
        }
    }

    @Override
    public Optional<XmvnArtifact> resolve(ArtifactKey key) {
        return Optional.ofNullable(byKey.get(key))
                .or(() -> Optional.ofNullable(byKey.get(key.withVersion(ArtifactKey.SYSTEM_VERSION))));
    }

    @Override
    public List<XmvnArtifact> artifacts() {
        return artifacts;
    }
}
