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

import org.altlinux.xgradle.impl.model.XmvnArtifact;

import java.nio.file.Path;
import java.util.List;

/**
 * Reads XMvn metadata files.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface MetadataReader {

    /**
     * Reads every metadata file under the given locations, in XMvn order:
     * locations as given, files inside a directory sorted by name.
     * Gzip-compressed files are accepted.
     *
     * @throws org.gradle.api.GradleException if a location is missing or a file cannot be parsed
     */
    List<XmvnArtifact> read(List<Path> locations);
}
