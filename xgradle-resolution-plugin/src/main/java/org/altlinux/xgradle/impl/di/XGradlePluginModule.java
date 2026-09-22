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

package org.altlinux.xgradle.impl.di;

import com.google.inject.AbstractModule;
import org.altlinux.xgradle.impl.caches.CachesModule;
import org.altlinux.xgradle.impl.collectors.CollectorsModule;
import org.altlinux.xgradle.impl.configurators.ConfiguratorsModule;
import org.altlinux.xgradle.impl.handlers.HandlersModule;
import org.altlinux.xgradle.impl.indexing.IndexingModule;
import org.altlinux.xgradle.impl.metadata.MetadataModule;
import org.altlinux.xgradle.impl.managers.ManagersModule;
import org.altlinux.xgradle.impl.maven.MavenModule;
import org.altlinux.xgradle.impl.parsers.ParsersModule;
import org.altlinux.xgradle.impl.processors.ProcessorsModule;
import org.altlinux.xgradle.impl.resolution.ResolutionModule;
import org.altlinux.xgradle.impl.resolvers.ResolversModule;
import org.altlinux.xgradle.impl.services.ServicesModule;
import org.altlinux.xgradle.impl.utils.logging.LoggingModule;
/**
 * Guice module for XGradle Plugin bindings.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class XGradlePluginModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LoggingModule());

        install(new CachesModule());

        install(new CollectorsModule());

        install(new ParsersModule());

        install(new IndexingModule());
        install(new MetadataModule());

        install(new ResolversModule());

        install(new ProcessorsModule());

        install(new ManagersModule());

        install(new ServicesModule());

        install(new MavenModule());

        install(new HandlersModule());

        install(new ConfiguratorsModule());

        install(new SbomModule());

        install(new ResolutionModule());
    }
}
