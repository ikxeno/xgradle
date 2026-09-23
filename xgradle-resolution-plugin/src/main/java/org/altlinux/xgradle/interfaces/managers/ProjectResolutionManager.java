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
 * See the License  for the specific language governing permissions and
 * limitations under the License.
 */
package org.altlinux.xgradle.interfaces.managers;

import org.gradle.api.initialization.Settings;

/**
 * Resolves the dependencies of every project from installed artifacts.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface ProjectResolutionManager extends Manager<Settings> {

    /**
     * Must run before the settings script, so the repository and the version rules
     * are in place before any script resolves a configuration.
     */
    @Override
    void configure(Settings settings);
}
