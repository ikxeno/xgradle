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

import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Artifact index with the semantics of XMvn's {@code DefaultMetadataResult}
 * (by default a key two artifacts claim is dropped, as with XMvn's default
 * {@code ignoreDuplicateMetadata=true}).
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class DefaultMetadataIndex implements MetadataIndex {

    private final List<XmvnArtifact> artifacts;
    private final Map<ArtifactKey, XmvnArtifact> byKey;
    private final List<String> conflicts = new ArrayList<>();

    /**
     * Indexes the artifacts in the given order.
     *
     * @param ignoreDuplicates XMvn's {@code ignoreDuplicateMetadata}: drop a key two
     *                         artifacts claim instead of letting the later one win
     */
    DefaultMetadataIndex(List<XmvnArtifact> artifacts, boolean ignoreDuplicates) {
        Map<ArtifactKey, XmvnArtifact> index = new LinkedHashMap<>();
        artifacts.forEach(artifact -> add(index, artifact, ignoreDuplicates));

        this.artifacts = List.copyOf(artifacts);
        this.byKey = Collections.unmodifiableMap(index);
    }

    /**
     * Adds an artifact under each of its keys, handling a key another artifact
     * already holds exactly like XMvn's {@code DefaultMetadataResult}: with
     * {@code ignoreDuplicates} the key is dropped, otherwise the later artifact wins
     * unless only the earlier one belongs to a namespace. As in XMvn, a key dropped
     * as a duplicate is free again for the next artifact that claims it.
     */
    private void add(Map<ArtifactKey, XmvnArtifact> index, XmvnArtifact artifact, boolean ignoreDuplicates) {
        Set<ArtifactKey> duplicates = new HashSet<>();
        artifact.lookupKeys().stream()
                .filter(key -> !duplicates.contains(key))
                .forEach(key -> {
                    XmvnArtifact existing = index.putIfAbsent(key, artifact);
                    if (existing == null) {
                        return;
                    }
                    duplicates.add(key);
                    if (ignoreDuplicates) {
                        index.remove(key);
                        conflicts.add("Ignoring XMvn metadata for " + key + ": it is provided by both "
                                + existing.getMetadataFile() + " and " + artifact.getMetadataFile());
                    } else {
                        conflicts.add("Duplicate XMvn metadata for " + key + ": "
                                + existing.getMetadataFile() + " and " + artifact.getMetadataFile());
                        if (existing.getNamespace().isEmpty() || !artifact.getNamespace().isEmpty()) {
                            index.put(key, artifact);
                        }
                    }
                });
    }

    /**
     * Keys two artifacts claimed, described for a warning.
     */
    List<String> conflicts() {
        return Collections.unmodifiableList(conflicts);
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

    @Override
    public Map<ArtifactKey, XmvnArtifact> entries() {
        return byKey;
    }
}
