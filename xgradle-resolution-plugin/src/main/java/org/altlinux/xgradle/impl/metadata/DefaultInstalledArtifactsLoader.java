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

import com.google.inject.Inject;

import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.interfaces.metadata.InstalledArtifactsLoader;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.metadata.MetadataReader;
import org.altlinux.xgradle.interfaces.metadata.PomArtifactReader;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * XMvn metadata is the primary source. A POM installed without metadata, as
 * xgradle-cli installs Gradle-built packages, adds a module only when no
 * metadata entry or alias already provides its groupId:artifactId.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class DefaultInstalledArtifactsLoader implements InstalledArtifactsLoader {

    private final MetadataReader metadataReader;
    private final PomArtifactReader pomReader;
    private final MetadataIndex index;

    @Inject
    DefaultInstalledArtifactsLoader(MetadataReader metadataReader, PomArtifactReader pomReader, MetadataIndex index) {
        this.metadataReader = metadataReader;
        this.pomReader = pomReader;
        this.index = index;
    }

    @Override
    public void load(List<Path> metadataLocations, boolean ignoreDuplicates, Path pomsRoot, Path javaRoot) {
        List<XmvnArtifact> fromMetadata = metadataReader.read(metadataLocations);
        // Loaded first so that parent POMs of the remaining POMs are found through the metadata.
        index.load(fromMetadata, ignoreDuplicates);

        Set<Path> describedFiles = fromMetadata.stream()
                .map(XmvnArtifact::getPath)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> describedModules = fromMetadata.stream()
                .flatMap(artifact -> artifact.lookupKeys().stream())
                .map(DefaultInstalledArtifactsLoader::module)
                .collect(Collectors.toSet());

        List<XmvnArtifact> fromPoms = pomReader.read(pomsRoot, javaRoot, describedFiles).stream()
                .filter(artifact -> !describedModules.contains(artifact.getGroupId() + ":" + artifact.getArtifactId()))
                .collect(Collectors.toList());

        index.load(Stream.concat(fromMetadata.stream(), fromPoms.stream()).collect(Collectors.toList()),
                ignoreDuplicates);
    }

    private static String module(ArtifactKey key) {
        return key.getGroupId() + ":" + key.getArtifactId();
    }
}
