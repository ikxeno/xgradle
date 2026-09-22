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

import java.util.Objects;

/**
 * Lookup key of an installed artifact, as XMvn builds it.
 * The version is either {@link #SYSTEM_VERSION} or a compat version.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class ArtifactKey {

    public static final String SYSTEM_VERSION = "SYSTEM";
    public static final String DEFAULT_EXTENSION = "jar";

    private final String groupId;
    private final String artifactId;
    private final String extension;
    private final String classifier;
    private final String version;

    public ArtifactKey(
            String groupId,
            String artifactId,
            String extension,
            String classifier,
            String version
    ) {
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        this.artifactId = Objects.requireNonNull(artifactId, "artifactId");
        this.extension = extension == null || extension.isEmpty() ? DEFAULT_EXTENSION : extension;
        this.classifier = classifier == null ? "" : classifier;
        this.version = version == null || version.isEmpty() ? SYSTEM_VERSION : version;
    }

    public static ArtifactKey jar(String groupId, String artifactId, String version) {
        return new ArtifactKey(groupId, artifactId, DEFAULT_EXTENSION, "", version);
    }

    public ArtifactKey withVersion(String newVersion) {
        return new ArtifactKey(groupId, artifactId, extension, classifier, newVersion);
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

    public String getVersion() {
        return version;
    }

    public boolean isSystemVersion() {
        return SYSTEM_VERSION.equals(version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactKey)) return false;
        ArtifactKey that = (ArtifactKey) o;
        return groupId.equals(that.groupId)
                && artifactId.equals(that.artifactId)
                && extension.equals(that.extension)
                && classifier.equals(that.classifier)
                && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, extension, classifier, version);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(groupId).append(':').append(artifactId).append(':').append(extension);
        if (!classifier.isEmpty()) {
            sb.append(':').append(classifier);
        }
        return sb.append(':').append(version).toString();
    }
}
