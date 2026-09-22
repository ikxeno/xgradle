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
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Finds installed modules in the XMvn metadata index, the way XMvn resolves
 * a dependency on the default (non-compat) version.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class MavenPomFinder implements PomFinder {

    private static final String POM = "pom";

    private final MetadataIndex index;

    @Inject
    MavenPomFinder(MetadataIndex index) {
        this.index = index;
    }

    @Override
    public MavenCoordinate findPomForArtifact(String groupId, String artifactId) {
        ArtifactKey jar = ArtifactKey.jar(groupId, artifactId, ArtifactKey.SYSTEM_VERSION);
        ArtifactKey pom = new ArtifactKey(groupId, artifactId, POM, "", ArtifactKey.SYSTEM_VERSION);

        Optional<XmvnArtifact> jarArtifact = index.resolve(jar).filter(artifact -> artifact.getPath() != null);
        Optional<XmvnArtifact> pomArtifact = index.resolve(pom);

        return jarArtifact.or(() -> pomArtifact)
                .map(artifact -> MavenCoordinate.builder()
                        .groupId(groupId)
                        .artifactId(artifactId)
                        .version(artifact.getVersion())
                        .packaging(jarArtifact.isPresent() ? ArtifactKey.DEFAULT_EXTENSION : POM)
                        .pomPath(pomArtifact.map(XmvnArtifact::getPath).orElse(null))
                        .build())
                .orElse(null);
    }

    @Override
    public List<MavenCoordinate> findAllPomsForGroup(String groupId) {
        return index.entries().keySet().stream()
                .filter(key -> key.getGroupId().equals(groupId) && key.isSystemVersion())
                .map(ArtifactKey::getArtifactId)
                .distinct()
                .map(artifactId -> findPomForArtifact(groupId, artifactId))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MavenCoordinate::getArtifactId))
                .collect(Collectors.toList());
    }
}
