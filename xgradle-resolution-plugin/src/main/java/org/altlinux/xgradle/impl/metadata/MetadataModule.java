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
package org.altlinux.xgradle.impl.metadata;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.altlinux.xgradle.impl.model.InstalledLayout;
import org.altlinux.xgradle.interfaces.metadata.IvyRepositoryGenerator;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.metadata.MetadataReader;
import org.altlinux.xgradle.interfaces.metadata.PomArtifactReader;
import org.altlinux.xgradle.interfaces.metadata.SystemRepository;

/**
 * Guice module for XMvn metadata bindings. The indexes are built once, on
 * first use, from the given layout.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class MetadataModule extends AbstractModule {

    private final InstalledLayout layout;

    public MetadataModule(InstalledLayout layout) {
        this.layout = layout;
    }

    @Override
    protected void configure() {
        bind(InstalledLayout.class).toInstance(layout);
        bind(MetadataReader.class).to(DefaultMetadataReader.class);
        bind(MetadataIndex.class).toProvider(InstalledIndexProvider.class).in(Singleton.class);
        bind(IvyRepositoryGenerator.class).to(DefaultIvyRepositoryGenerator.class);
        bind(SystemRepository.class).to(DefaultSystemRepository.class);
        bind(PomArtifactReader.class).to(DefaultPomArtifactReader.class);
    }

    @Provides
    @Singleton
    @Named(MetadataIndex.XMVN_METADATA)
    MetadataIndex xmvnIndex(MetadataReader reader) {
        return new DefaultMetadataIndex(reader.read(layout.getMetadataLocations()), layout.isIgnoreDuplicateMetadata());
    }
}
