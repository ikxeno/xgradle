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
    private final boolean optional;
    private final List<String> exclusions;

    private XmvnDependency(Builder builder) {
        this.groupId = Objects.requireNonNull(builder.groupId, "groupId");
        this.artifactId = Objects.requireNonNull(builder.artifactId, "artifactId");
        this.extension = isEmpty(builder.extension) ? ArtifactKey.DEFAULT_EXTENSION : builder.extension;
        this.classifier = builder.classifier == null ? "" : builder.classifier;
        this.requestedVersion = isEmpty(builder.requestedVersion)
                ? ArtifactKey.SYSTEM_VERSION
                : builder.requestedVersion;
        this.optional = builder.optional;
        this.exclusions = List.copyOf(builder.exclusions);
    }

    public static Builder builder(String groupId, String artifactId) {
        return new Builder(groupId, artifactId);
    }

    /**
     * Key XMvn uses to look this dependency up: its effective POM carries the
     * requested version, which the resolver matches against compat versions
     * before falling back to {@link ArtifactKey#SYSTEM_VERSION}. The resolved
     * version XMvn also records only says what it resolved to at install time,
     * so it is not read.
     */
    public ArtifactKey toKey() {
        return new ArtifactKey(groupId, artifactId, extension, classifier, requestedVersion);
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

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static final class Builder {

        private final String groupId;
        private final String artifactId;
        private String extension;
        private String classifier;
        private String requestedVersion;
        private boolean optional;
        private List<String> exclusions = List.of();

        private Builder(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        /** Default {@code jar}. */
        public Builder extension(String extension) {
            this.extension = extension;
            return this;
        }

        public Builder classifier(String classifier) {
            this.classifier = classifier;
            return this;
        }

        /** Default {@link ArtifactKey#SYSTEM_VERSION}. */
        public Builder requestedVersion(String requestedVersion) {
            this.requestedVersion = requestedVersion;
            return this;
        }

        public Builder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public Builder exclusions(List<String> exclusions) {
            this.exclusions = exclusions;
            return this;
        }

        public XmvnDependency build() {
            return new XmvnDependency(this);
        }
    }
}
