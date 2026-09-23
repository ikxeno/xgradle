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

import org.altlinux.xgradle.impl.model.IvyRepository;

import java.nio.file.Path;

/**
 * Turns the XMvn metadata index into an ivy repository Gradle can resolve
 * transitive dependencies from.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface IvyRepositoryGenerator {

    String IVY_PATTERN = "[organisation]/[module]/[revision]/ivy.xml";
    String ARTIFACT_PATTERN = "[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]";

    /**
     * Writes the repository for the current index under the cache directory,
     * or reuses one written earlier for the same metadata.
     */
    IvyRepository generate(Path cacheDirectory);
}
