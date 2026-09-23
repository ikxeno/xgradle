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

import org.gradle.api.invocation.Gradle;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    public void configure(
            Gradle gradle,
            Map<String, Set<String>> requestedVersions,
            Map<String, String> overrideLogs
    ) {
        requestedVersions.forEach((key, versions) -> {
            String[] ga = key.split(":", 2);
            versions.stream()
                    .filter(Objects::nonNull)
                    .forEach(version -> index.moduleRevision(ga[0], ga[1], version)
                            .filter(revision -> !revision.equals(version))
                            .ifPresent(revision -> overrideLogs.put(
                                    key + "|" + version + "|" + revision,
                                    "Override version: " + key + ":" + version + " -> " + revision)));
        });

        gradle.allprojects(project -> project.getConfigurations().configureEach(configuration ->
                configuration.getResolutionStrategy().eachDependency(details -> {
                    String requested = details.getRequested().getVersion();
                    index.moduleRevision(details.getRequested().getGroup(), details.getRequested().getName(), requested)
                            .filter(revision -> !revision.equals(requested))
                            .ifPresent(revision -> {
                                details.useVersion(revision);
                                details.because(REASON);
                            });
                })));
    }
}
