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

import java.nio.file.Path;
import java.util.List;

/**
 * Fills the {@link MetadataIndex} with everything installed: artifacts described
 * by XMvn metadata, plus artifacts that only have a POM.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface InstalledArtifactsLoader {

    /**
     * @param metadataLocations XMvn metadata files or directories
     * @param ignoreDuplicates  XMvn's {@code ignoreDuplicateMetadata}
     * @param pomsRoot          root of installed POMs, e.g. {@code /usr/share/maven-poms}
     * @param javaRoot          root of installed jars, e.g. {@code /usr/share/java}
     */
    void load(List<Path> metadataLocations, boolean ignoreDuplicates, Path pomsRoot, Path javaRoot);
}
