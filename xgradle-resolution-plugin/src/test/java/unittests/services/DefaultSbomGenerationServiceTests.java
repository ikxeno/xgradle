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
package unittests.services;

import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.impl.services.DefaultSbomGenerationService;
import org.altlinux.xgradle.interfaces.collectors.SbomComponentCollector;
import org.altlinux.xgradle.interfaces.generators.SbomGenerator;
import org.gradle.api.Project;
import org.gradle.api.GradleException;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultSbomGenerationService contract")
class DefaultSbomGenerationServiceTests {

    @Mock
    private SbomGenerator sbomGenerator;

    @Mock
    private SbomComponentCollector sbomComponentCollector;

    @Mock
    private Gradle gradle;

    @Mock
    private Logger logger;

    @Test
    @DisplayName("Generates SBOM and logs output path")
    void generatesSbomAndLogsOutputPath() {
        Project root = ProjectBuilder.builder().withName("demo-root").build();
        root.setVersion("1.2.3");
        when(gradle.getRootProject()).thenReturn(root);

        MavenCoordinate coordinate = MavenCoordinate.builder()
                .groupId("org.example")
                .artifactId("core")
                .version("1.0.0")
                .build();

        Collection<SbomComponent> components = List.of(
                SbomComponent.maven("org.example", "core", "1.0.0")
        );
        when(sbomComponentCollector.collect(anyCollection(), anyCollection(), anyCollection()))
                .thenReturn((List<SbomComponent>) components);

        DefaultSbomGenerationService service =
                new DefaultSbomGenerationService(sbomGenerator, sbomComponentCollector);
        service.generate(
                gradle,
                SbomFormat.CYCLONEDX,
                Map.of("org.example:core", coordinate),
                List.of(),
                List.of(),
                logger
        );

        verify(sbomGenerator).generate(
                eq(SbomFormat.CYCLONEDX),
                argThat(path -> path.toString().endsWith(
                        "build/reports/xgradle/sbom-cyclonedx.json"
                )),
                eq("demo-root"),
                eq("1.2.3"),
                eq(components)
        );
        verify(logger).lifecycle(eq("Generated {} SBOM: {}"), eq("cyclonedx"), any(Path.class));
    }

    @Test
    @DisplayName("Fails the build when the requested SBOM cannot be generated")
    void failsWhenSbomGenerationFails() {
        Project root = ProjectBuilder.builder().withName("demo-root").build();
        when(gradle.getRootProject()).thenReturn(root);
        when(sbomComponentCollector.collect(anyCollection(), anyCollection(), anyCollection()))
                .thenReturn(List.of());
        doThrow(new RuntimeException("boom")).when(sbomGenerator).generate(
                any(),
                any(),
                anyString(),
                anyString(),
                anyCollection()
        );

        DefaultSbomGenerationService service =
                new DefaultSbomGenerationService(sbomGenerator, sbomComponentCollector);

        GradleException e = assertThrows(GradleException.class, () -> service.generate(
                gradle,
                SbomFormat.SPDX,
                Map.of(),
                List.of(),
                List.of(),
                logger
        ));

        assertEquals("boom", e.getCause().getMessage());
    }
}
