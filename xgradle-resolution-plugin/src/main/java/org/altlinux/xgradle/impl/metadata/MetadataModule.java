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

import org.altlinux.xgradle.interfaces.metadata.InstalledArtifactsLoader;
import org.altlinux.xgradle.interfaces.metadata.IvyRepositoryGenerator;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.metadata.MetadataReader;
import org.altlinux.xgradle.interfaces.metadata.PomArtifactReader;

/**
 * Guice module for XMvn metadata bindings.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class MetadataModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MetadataReader.class).to(DefaultMetadataReader.class);
        bind(MetadataIndex.class).to(DefaultMetadataIndex.class);
        bind(IvyRepositoryGenerator.class).to(DefaultIvyRepositoryGenerator.class);
        bind(PomArtifactReader.class).to(DefaultPomArtifactReader.class);
        bind(InstalledArtifactsLoader.class).to(DefaultInstalledArtifactsLoader.class);
    }
}
