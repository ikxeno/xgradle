/*
 * Copyright 2025 BaseALT Ltd
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
package org.altlinux.xgradle.interfaces.collectors;

import org.gradle.api.initialization.Settings;

import java.io.File;
import java.util.Set;

/**
 * Collects the jars the build resolves, transitive ones and script classpaths
 * included, for the SBOM.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface ResolvedJarsCollector {

    /**
     * Starts collecting from the settings script, every project script and every
     * project configuration, if an SBOM is requested. Must run before the settings script.
     */
    void watch(Settings settings);

    /**
     * The jars collected so far.
     */
    Set<File> jars();
}
