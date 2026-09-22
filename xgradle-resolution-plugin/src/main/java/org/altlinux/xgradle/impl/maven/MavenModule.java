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

package org.altlinux.xgradle.impl.maven;

import com.google.inject.AbstractModule;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.maven.PomHierarchyLoader;
/**
 * Guice module for Maven bindings.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class MavenModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PomFinder.class).to(MavenPomFinder.class);
        bind(PomHierarchyLoader.class).to(MavenPomHierarchyLoader.class);
    }
}
