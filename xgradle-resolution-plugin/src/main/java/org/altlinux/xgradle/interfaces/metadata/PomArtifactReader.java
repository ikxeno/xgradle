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
import java.util.Set;

/**
 * Describes artifacts installed without XMvn metadata, such as packages built
 * with Gradle and installed by xgradle-cli, from their POM files.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface PomArtifactReader {

    /**
     * Reads every POM under {@code pomsRoot} except those in {@code skip}, pairing
     * {@code pomsRoot/X/Y.pom} with the jar {@code javaRoot/X/Y.jar} if it exists.
     */
    List<XmvnArtifact> read(Path pomsRoot, Path javaRoot, Set<Path> skip);
}
