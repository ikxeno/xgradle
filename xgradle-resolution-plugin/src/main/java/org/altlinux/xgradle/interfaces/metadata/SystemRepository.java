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
package org.altlinux.xgradle.interfaces.metadata;

import org.altlinux.xgradle.impl.model.IvyRepository;

import org.gradle.api.invocation.Gradle;

import java.util.Optional;

/**
 * The ivy repository of installed artifacts for a build.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface SystemRepository {

    /**
     * The repository in the build's Gradle user home, or empty if nothing is installed.
     */
    Optional<IvyRepository> forBuild(Gradle gradle);
}
