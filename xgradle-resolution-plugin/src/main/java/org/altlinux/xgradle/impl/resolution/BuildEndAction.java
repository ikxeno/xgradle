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
package org.altlinux.xgradle.impl.resolution;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * Runs an action when the build ends. Gradle closes shared build services at
 * the end of the build, which replaces {@code Gradle.buildFinished}, removed in
 * Gradle 10.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public abstract class BuildEndAction implements BuildService<BuildServiceParameters.None>, AutoCloseable {

    private Runnable action;

    void setAction(Runnable action) {
        this.action = action;
    }

    @Override
    public void close() {
        if (action != null) {
            action.run();
        }
    }
}
