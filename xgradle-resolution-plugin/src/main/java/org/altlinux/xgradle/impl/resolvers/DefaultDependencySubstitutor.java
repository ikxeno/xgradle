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
package org.altlinux.xgradle.impl.resolvers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.resolvers.DependencySubstitutor;

import org.gradle.api.artifacts.ConfigurationContainer;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles dependency version substitutions during Gradle resolution.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
public final class DefaultDependencySubstitutor implements DependencySubstitutor {

    private static final String REASON = "Installed system version (XMvn metadata)";

    private final MetadataIndex index;

    @Inject
    public DefaultDependencySubstitutor(MetadataIndex index) {
        this.index = index;
    }

    @Override
    public void configure(ConfigurationContainer configurations) {
        configurations.configureEach(configuration ->
                configuration.getResolutionStrategy().eachDependency(details -> {
                    String requested = details.getRequested().getVersion();
                    index.moduleRevision(details.getRequested().getGroup(), details.getRequested().getName(), requested)
                            .filter(revision -> !revision.equals(requested))
                            .ifPresent(revision -> {
                                details.useVersion(revision);
                                details.because(REASON);
                            });
                }));
    }

    @Override
    public Map<String, String> overrides(Map<String, Set<String>> requestedVersions) {
        return requestedVersions.entrySet().stream()
                .flatMap(entry -> {
                    String[] ga = entry.getKey().split(":", 2);
                    return entry.getValue().stream()
                            .filter(Objects::nonNull)
                            .flatMap(version -> index.moduleRevision(ga[0], ga[1], version)
                                    .filter(revision -> !revision.equals(version))
                                    .map(revision -> Map.entry(
                                            entry.getKey() + "|" + version + "|" + revision,
                                            "Override version: " + entry.getKey() + ":" + version + " -> " + revision))
                                    .stream());
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first));
    }
}
