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
package org.altlinux.xgradle.impl.model;

import java.util.List;
import java.util.Objects;

/**
 * Dependency of an installed artifact, as recorded in XMvn metadata.
 * XMvn keeps only compile and runtime dependencies, so there is no scope.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class XmvnDependency {

    private final String groupId;
    private final String artifactId;
    private final String extension;
    private final String classifier;
    private final String requestedVersion;
    private final String resolvedVersion;
    private final String namespace;
    private final boolean optional;
    private final List<String> exclusions;

    public XmvnDependency(
            String groupId,
            String artifactId,
            String extension,
            String classifier,
            String requestedVersion,
            String resolvedVersion,
            String namespace,
            boolean optional,
            List<String> exclusions
    ) {
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        this.artifactId = Objects.requireNonNull(artifactId, "artifactId");
        this.extension = extension == null || extension.isEmpty() ? ArtifactKey.DEFAULT_EXTENSION : extension;
        this.classifier = classifier == null ? "" : classifier;
        this.requestedVersion = requestedVersion == null ? ArtifactKey.SYSTEM_VERSION : requestedVersion;
        this.resolvedVersion = resolvedVersion == null ? ArtifactKey.SYSTEM_VERSION : resolvedVersion;
        this.namespace = namespace == null ? "" : namespace;
        this.optional = optional;
        this.exclusions = List.copyOf(exclusions);
    }

    /**
     * Key XMvn uses to look this dependency up: the resolved version, which is
     * {@link ArtifactKey#SYSTEM_VERSION} unless the build picked a compat version.
     */
    public ArtifactKey toKey() {
        return new ArtifactKey(groupId, artifactId, extension, classifier, resolvedVersion);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getExtension() {
        return extension;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getRequestedVersion() {
        return requestedVersion;
    }

    public String getResolvedVersion() {
        return resolvedVersion;
    }

    public String getNamespace() {
        return namespace;
    }

    public boolean isOptional() {
        return optional;
    }

    /**
     * Excluded modules as {@code groupId:artifactId}; either part may be {@code *}.
     */
    public List<String> getExclusions() {
        return exclusions;
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + requestedVersion;
    }
}
