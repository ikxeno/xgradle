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

/**
 * Where the system keeps what is installed, and how XMvn is configured to read it.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class InstalledLayout {

    private final List<Path> metadataLocations;
    private final boolean ignoreDuplicateMetadata;
    private final Path pomsRoot;
    private final Path javaRoot;

    /**
     * @param metadataLocations       XMvn metadata files or directories
     * @param ignoreDuplicateMetadata XMvn's {@code ignoreDuplicateMetadata}
     * @param pomsRoot                root of installed POMs, e.g. {@code /usr/share/maven-poms}
     * @param javaRoot                root of installed jars, e.g. {@code /usr/share/java}
     */
    public InstalledLayout(List<Path> metadataLocations, boolean ignoreDuplicateMetadata,
                           Path pomsRoot, Path javaRoot) {
        this.metadataLocations = List.copyOf(metadataLocations);
        this.ignoreDuplicateMetadata = ignoreDuplicateMetadata;
        this.pomsRoot = pomsRoot;
        this.javaRoot = javaRoot;
    }

    public List<Path> getMetadataLocations() {
        return metadataLocations;
    }

    public boolean isIgnoreDuplicateMetadata() {
        return ignoreDuplicateMetadata;
    }

    public Path getPomsRoot() {
        return pomsRoot;
    }

    public Path getJavaRoot() {
        return javaRoot;
    }
}
