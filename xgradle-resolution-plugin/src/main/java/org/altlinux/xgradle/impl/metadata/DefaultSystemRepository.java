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
package org.altlinux.xgradle.impl.metadata;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.extensions.SystemDepsExtension;
import org.altlinux.xgradle.impl.model.IvyRepository;
import org.altlinux.xgradle.interfaces.metadata.IvyRepositoryGenerator;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.metadata.SystemRepository;

import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Generates the repository once there is something installed, and warns once when
 * there is not.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultSystemRepository implements SystemRepository {

    private final MetadataIndex index;
    private final IvyRepositoryGenerator generator;
    private final Logger logger;
    private final AtomicBoolean emptyReported = new AtomicBoolean();

    @Inject
    DefaultSystemRepository(MetadataIndex index, IvyRepositoryGenerator generator, Logger logger) {
        this.index = index;
        this.generator = generator;
        this.logger = logger;
    }

    @Override
    public Optional<IvyRepository> forBuild(Gradle gradle) {
        if (index.artifacts().isEmpty()) {
            if (emptyReported.compareAndSet(false, true)) {
                logger.warn("No installed artifacts found in XMvn metadata; nothing is resolved from the system");
            }
            return Optional.empty();
        }
        return Optional.of(generator.generate(SystemDepsExtension.getIvyCacheDir(gradle)));
    }
}
