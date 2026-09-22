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
     */
    void build(List<Path> locations);

    /**
     * Resolves a key like XMvn: an exact compat version match first,
     * then the default artifact under {@link ArtifactKey#SYSTEM_VERSION}.
     */
    Optional<XmvnArtifact> resolve(ArtifactKey key);

    /**
     * All installed artifacts, in metadata read order.
     */
    List<XmvnArtifact> artifacts();
}
