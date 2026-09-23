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

import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Immutable index of installed artifacts. Lookups follow the rules of the XMvn resolver.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface MetadataIndex {

    /**
     * Name of the binding that holds only the artifacts described by XMvn metadata,
     * without POMs installed without metadata. Parent POM lookup uses it, since the
     * full index is built by reading those POMs.
     */
    String XMVN_METADATA = "xmvnMetadata";

    /**
     * Resolves a key like XMvn: an exact compat version match first,
     * then the default artifact under {@link ArtifactKey#SYSTEM_VERSION}.
     */
    Optional<XmvnArtifact> resolve(ArtifactKey key);

    /**
     * Revision of the installed module a key resolves to: the requested compat
     * version if it is installed, otherwise the upstream version of the default artifact.
     */
    default Optional<String> revision(ArtifactKey key) {
        if (!key.isSystemVersion() && entries().containsKey(key)) {
            return Optional.of(key.getVersion());
        }
        return resolve(key.withVersion(ArtifactKey.SYSTEM_VERSION)).map(XmvnArtifact::getVersion);
    }

    /**
     * Main artifact of a module: its jar, or its POM if the module has no installed
     * jar, as a parent, a BOM or a Gradle plugin marker. An entry without a file does
     * not count as installed.
     */
    default Optional<XmvnArtifact> resolveModule(String groupId, String artifactId, String version) {
        return moduleKeys(groupId, artifactId, version)
                .map(this::resolve)
                .flatMap(Optional::stream)
                .filter(artifact -> artifact.getPath() != null)
                .findFirst();
    }

    /**
     * Main artifact of the installed module a dependency declared with the given
     * versions resolves to: a compat version that exactly matches one of them, else
     * the system version.
     */
    default Optional<XmvnArtifact> resolveModule(String groupId, String artifactId,
                                                 Collection<String> requestedVersions) {
        return Stream.concat(
                        requestedVersions.stream().filter(Objects::nonNull).sorted(),
                        Stream.of(ArtifactKey.SYSTEM_VERSION))
                .flatMap(version -> moduleKeys(groupId, artifactId, version))
                .map(entries()::get)
                .filter(Objects::nonNull)
                .filter(artifact -> artifact.getPath() != null)
                .findFirst();
    }

    /**
     * Path of the POM installed with the given artifact, for its own version.
     */
    default Optional<Path> pomOf(XmvnArtifact artifact) {
        return artifact.lookupKeys().stream()
                .filter(key -> key.getGroupId().equals(artifact.getGroupId())
                        && key.getArtifactId().equals(artifact.getArtifactId()))
                .map(key -> new ArtifactKey(
                        key.getGroupId(), key.getArtifactId(), ArtifactKey.POM_EXTENSION, "", key.getVersion()))
                .map(entries()::get)
                .filter(Objects::nonNull)
                .map(XmvnArtifact::getPath)
                .filter(Objects::nonNull)
                .findFirst();
    }

    /**
     * Revision of the installed module a request resolves to, as {@link #revision}
     * resolves it for the jar or, for a POM-only module, the POM.
     */
    default Optional<String> moduleRevision(String groupId, String artifactId, String version) {
        return moduleKeys(groupId, artifactId, version)
                .map(this::revision)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Stream<ArtifactKey> moduleKeys(String groupId, String artifactId, String version) {
        return Stream.of(ArtifactKey.DEFAULT_EXTENSION, ArtifactKey.POM_EXTENSION)
                .map(extension -> new ArtifactKey(groupId, artifactId, extension, "", version));
    }

    /**
     * The installed artifact whose file the given path is, directly or through symlinks.
     */
    Optional<XmvnArtifact> artifactAt(Path file);

    /**
     * All installed artifacts, in metadata read order.
     */
    List<XmvnArtifact> artifacts();

    /**
     * Every lookup key with the artifact that won it, in index order.
     */
    Map<ArtifactKey, XmvnArtifact> entries();
}
