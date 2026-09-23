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
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.interfaces.maven.ModuleFinder;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Finds installed modules in the XMvn metadata index by their system
 * (non-compat) version.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class IndexModuleFinder implements ModuleFinder {

    private final MetadataIndex index;

    @Inject
    IndexModuleFinder(MetadataIndex index) {
        this.index = index;
    }

    @Override
    public Optional<MavenCoordinate> findModule(String groupId, String artifactId,
                                                Collection<String> requestedVersions) {
        return index.resolveModule(groupId, artifactId, requestedVersions)
                .map(artifact -> MavenCoordinate.builder()
                        .groupId(groupId)
                        .artifactId(artifactId)
                        .version(artifact.getVersion())
                        .packaging(artifact.getExtension())
                        .pomPath(index.pomOf(artifact).orElse(null))
                        .build());
    }

    @Override
    public List<MavenCoordinate> findModulesOfGroup(String groupId) {
        return index.entries().keySet().stream()
                .filter(key -> key.getGroupId().equals(groupId) && key.isSystemVersion())
                .map(ArtifactKey::getArtifactId)
                .distinct()
                .map(artifactId -> findModule(groupId, artifactId))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(MavenCoordinate::getArtifactId))
                .collect(Collectors.toList());
    }
}
