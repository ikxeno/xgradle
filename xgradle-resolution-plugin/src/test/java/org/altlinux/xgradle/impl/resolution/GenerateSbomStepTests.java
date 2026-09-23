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
package org.altlinux.xgradle.impl.resolution;

import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.interfaces.collectors.ResolvedJarsCollector;
import org.altlinux.xgradle.interfaces.processors.PluginProcessor;
import org.altlinux.xgradle.interfaces.services.SbomGenerationService;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.services.BuildServiceRegistry;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateSbomStep")
class GenerateSbomStepTests {

    @Mock
    private SbomGenerationService sbomGenerationService;

    @Mock
    private PluginProcessor pluginProcessor;

    @Mock
    private Gradle gradle;

    @Mock
    private Project rootProject;

    @Mock
    private Logger logger;

    @AfterEach
    void clearSbomProperty() {
        System.clearProperty("generate.sbom");
    }

    @Test
    @DisplayName("Snapshots dependencies and delegates generation with plugin artifacts")
    void snapshotsDependenciesAndDelegatesGenerationWithPluginArtifacts() {
        System.setProperty("generate.sbom", "spdx");

        MavenCoordinate dependency = MavenCoordinate.builder()
                .groupId("org.example")
                .artifactId("core-lib")
                .version("1.2.3")
                .build();

        MavenCoordinate pluginArtifact = MavenCoordinate.builder()
                .groupId("com.acme.plugin")
                .artifactId("awesome-gradle-plugin")
                .version("2.0.0")
                .build();

        when(gradle.getRootProject()).thenReturn(rootProject);
        when(rootProject.getLogger()).thenReturn(logger);
        when(pluginProcessor.getResolvedPluginArtifacts()).thenReturn(List.of(pluginArtifact));

        BuildEndAction buildEnd = new BuildEndAction() {
            @Override
            public BuildServiceParameters.None getParameters() {
                return null;
            }
        };
        BuildServiceRegistry sharedServices = mock(BuildServiceRegistry.class);
        @SuppressWarnings("unchecked")
        Provider<BuildEndAction> provider = mock(Provider.class);
        when(gradle.getSharedServices()).thenReturn(sharedServices);
        when(sharedServices.registerIfAbsent(anyString(), eq(BuildEndAction.class), any())).thenReturn(provider);
        when(provider.get()).thenReturn(buildEnd);

        ResolutionContext resolutionContext = new ResolutionContext(gradle);
        resolutionContext.putSystemArtifact("org.example:core-lib", dependency);

        GenerateSbomStep step = new GenerateSbomStep(
                sbomGenerationService,
                pluginProcessor,
                mock(ResolvedJarsCollector.class)
        );
        step.execute(resolutionContext);
        verify(sbomGenerationService, never()).generate(any(), any(), any(), any(), any());
        buildEnd.close();

        verify(sbomGenerationService).generate(
                eq(gradle),
                eq(SbomFormat.SPDX),
                argThat(this::containsDependency),
                argThat(plugins -> containsCoordinate(
                        plugins,
                        "com.acme.plugin",
                        "awesome-gradle-plugin",
                        "2.0.0"
                )),
                any()
        );
    }

    private boolean containsDependency(Collection<MavenCoordinate> artifactsSnapshot) {
        return containsCoordinate(artifactsSnapshot, "org.example", "core-lib", "1.2.3");
    }

    private boolean containsCoordinate(
            Collection<MavenCoordinate> artifacts,
            String groupId,
            String artifactId,
            String version
    ) {
        if (artifacts == null) {
            return false;
        }

        for (MavenCoordinate artifact : artifacts) {
            if (artifact == null) {
                continue;
            }
            if (groupId.equals(artifact.getGroupId())
                    && artifactId.equals(artifact.getArtifactId())
                    && version.equals(artifact.getVersion())) {
                return true;
            }
        }
        return false;
    }
}
