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

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Installed artifact, as an XMvn metadata file ({@code /usr/share/maven-metadata/*.xml})
 * describes it, or as read from a POM installed without metadata.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class XmvnArtifact {

    private final String groupId;
    private final String artifactId;
    private final String extension;
    private final String classifier;
    private final String version;
    private final Path path;
    private final String namespace;
    private final List<String> compatVersions;
    private final List<ArtifactKey> aliases;
    private final List<XmvnDependency> dependencies;
    private final Path metadataFile;

    private XmvnArtifact(Builder builder) {
        this.groupId = Objects.requireNonNull(builder.groupId, "groupId");
        this.artifactId = Objects.requireNonNull(builder.artifactId, "artifactId");
        this.version = Objects.requireNonNull(builder.version, "version");
        this.extension = builder.extension == null || builder.extension.isEmpty()
                ? ArtifactKey.DEFAULT_EXTENSION
                : builder.extension;
        this.classifier = builder.classifier == null ? "" : builder.classifier;
        this.path = builder.path;
        this.namespace = builder.namespace == null ? "" : builder.namespace;
        this.compatVersions = List.copyOf(builder.compatVersions);
        this.aliases = List.copyOf(builder.aliases);
        this.dependencies = List.copyOf(builder.dependencies);
        this.metadataFile = builder.metadataFile;
    }

    /**
     * @param metadataFile the metadata file or POM the artifact was read from
     */
    public static Builder builder(String groupId, String artifactId, String version, Path metadataFile) {
        return new Builder(groupId, artifactId, version, metadataFile);
    }

    /**
     * Keys this artifact is reachable under: its own coordinates and every alias,
     * each with every compat version, or with {@link ArtifactKey#SYSTEM_VERSION}
     * if it is not a compat artifact.
     */
    public List<ArtifactKey> lookupKeys() {
        List<String> versions = compatVersions.isEmpty()
                ? List.of(ArtifactKey.SYSTEM_VERSION)
                : compatVersions;
        ArtifactKey self = new ArtifactKey(groupId, artifactId, extension, classifier, ArtifactKey.SYSTEM_VERSION);
        return Stream.concat(Stream.of(self), aliases.stream())
                .flatMap(base -> versions.stream().map(base::withVersion))
                .distinct()
                .collect(Collectors.toList());
    }

    public boolean isCompat() {
        return !compatVersions.isEmpty();
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

    /**
     * Upstream version; never a compat version and never {@code SYSTEM}.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Absolute path of the installed file; {@code null} if the metadata has none.
     */
    public Path getPath() {
        return path;
    }

    public String getNamespace() {
        return namespace;
    }

    public List<XmvnDependency> getDependencies() {
        return dependencies;
    }

    /**
     * The metadata file or POM this artifact was read from.
     */
    public Path getMetadataFile() {
        return metadataFile;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(groupId).append(':').append(artifactId).append(':').append(extension);
        if (!classifier.isEmpty()) {
            sb.append(':').append(classifier);
        }
        return sb.append(':').append(version).toString();
    }

    public static final class Builder {

        private final String groupId;
        private final String artifactId;
        private final String version;
        private final Path metadataFile;
        private String extension;
        private String classifier;
        private Path path;
        private String namespace;
        private List<String> compatVersions = List.of();
        private List<ArtifactKey> aliases = List.of();
        private List<XmvnDependency> dependencies = List.of();

        private Builder(String groupId, String artifactId, String version, Path metadataFile) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.metadataFile = metadataFile;
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

        /** Absolute path of the installed file. */
        public Builder path(Path path) {
            this.path = path;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder compatVersions(List<String> compatVersions) {
            this.compatVersions = compatVersions;
            return this;
        }

        /**
         * Other coordinates of the same artifact, such as its groupId before a rename.
         * Their version is ignored: an alias is reachable under the artifact's versions.
         */
        public Builder aliases(List<ArtifactKey> aliases) {
            this.aliases = aliases;
            return this;
        }

        public Builder dependencies(List<XmvnDependency> dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        public XmvnArtifact build() {
            return new XmvnArtifact(this);
        }
    }
}
