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
package org.altlinux.xgradle.interfaces.metadata;

import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.XmvnArtifact;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Index of installed artifacts built from XMvn metadata, resolving
 * artifacts the same way the XMvn resolver does.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface MetadataIndex {

    /**
     * Reads the metadata under the given locations and replaces the index content.
     *
     * @param ignoreDuplicates XMvn's {@code ignoreDuplicateMetadata}: drop a key two
     *                         artifacts claim instead of letting the later one win
     */
    void build(List<Path> locations, boolean ignoreDuplicates);

    /**
     * Builds the index with XMvn's default, {@code ignoreDuplicateMetadata=true}.
     */
    default void build(List<Path> locations) {
        build(locations, true);
    }

    /**
     * Resolves a key like XMvn: an exact compat version match first,
     * then the default artifact under {@link ArtifactKey#SYSTEM_VERSION}.
     */
    Optional<XmvnArtifact> resolve(ArtifactKey key);

    /**
     * Revision of the installed module a key resolves to, as XMvn resolves it:
     * the requested compat version if one is installed, otherwise the upstream
     * version of the default artifact.
     */
    default Optional<String> revision(ArtifactKey key) {
        if (!key.isSystemVersion() && entries().containsKey(key)) {
            return Optional.of(key.getVersion());
        }
        return resolve(key.withVersion(ArtifactKey.SYSTEM_VERSION)).map(XmvnArtifact::getVersion);
    }

    /**
     * All installed artifacts, in metadata read order.
     */
    List<XmvnArtifact> artifacts();

    /**
     * Every lookup key with the artifact that won it, in index order.
     */
    Map<ArtifactKey, XmvnArtifact> entries();
}
