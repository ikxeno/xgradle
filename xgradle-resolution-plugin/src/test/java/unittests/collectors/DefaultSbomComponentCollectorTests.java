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
package unittests.collectors;

import org.altlinux.xgradle.impl.collectors.DefaultSbomComponentCollector;
import org.altlinux.xgradle.impl.enums.SbomComponentKind;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.services.PomMetadata;
import org.altlinux.xgradle.interfaces.services.PomMetadataLicense;
import org.altlinux.xgradle.interfaces.services.PomMetadataReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultSbomComponentCollector contract")
class DefaultSbomComponentCollectorTests {

    @Mock
    private PomMetadataReader pomMetadataReader;

    @Mock
    private MetadataIndex index;


    @Test
    @DisplayName("Collects library plugin and resolved jar components")
    void collectsLibraryPluginAndResolvedJarComponents(@TempDir Path tempDir) throws Exception {
        Path resolvedJar = Files.createFile(tempDir.resolve("resolved-extra.jar"));

        Path pomPath = Files.createFile(tempDir.resolve("artifact.pom"));
        when(pomMetadataReader.read(pomPath)).thenReturn(new PomMetadata(
                "https://project.example",
                "https://scm.example",
                List.of(new PomMetadataLicense(" Apache-2.0 ", " https://apache.org "))
        ));

        MavenCoordinate library = coordinate(
                "org.example",
                "core-lib",
                "1.0.0",
                "jar",
                pomPath
        );
        MavenCoordinate plugin = coordinate(
                "org.example",
                "gradle-plugin",
                "1.0.0",
                "jar",
                pomPath
        );
        MavenCoordinate bom = coordinate(
                "org.example",
                "core-bom",
                "1.0.0",
                "pom",
                pomPath
        );

        DefaultSbomComponentCollector collector = new DefaultSbomComponentCollector(pomMetadataReader, index);
        List<SbomComponent> components = collector.collect(
                List.of(library, bom),
                List.of(plugin),
                List.of(resolvedJar.toFile())
        );

        assertEquals(3, components.size());

        SbomComponent libraryComponent = components.stream()
                .filter(component -> "core-lib".equals(component.getArtifactId()))
                .findFirst()
                .orElseThrow();
        SbomComponent pluginComponent = components.stream()
                .filter(component -> "gradle-plugin".equals(component.getArtifactId()))
                .findFirst()
                .orElseThrow();
        SbomComponent fileComponent = components.stream()
                .filter(component -> component.getComponentKind() == SbomComponentKind.FILE)
                .findFirst()
                .orElseThrow();

        assertAll(
                () -> assertEquals(SbomComponentKind.LIBRARY, libraryComponent.getComponentKind()),
                () -> assertEquals("https://project.example", libraryComponent.getProjectUrl()),
                () -> assertEquals("https://scm.example", libraryComponent.getScmUrl()),
                () -> assertEquals(1, libraryComponent.getLicenses().size()),
                () -> assertEquals("Apache-2.0", libraryComponent.getLicenses().get(0).getName()),
                () -> assertEquals(SbomComponentKind.GRADLE_PLUGIN, pluginComponent.getComponentKind()),
                () -> assertEquals("resolved-extra.jar", fileComponent.getFileName())
        );

        verify(pomMetadataReader, times(1)).read(pomPath);
    }

    @Test
    @DisplayName("Reports a transitive jar of an installed artifact with its coordinates")
    void reportsInstalledTransitiveJar(@TempDir Path tempDir) throws Exception {
        Path installedJar = Files.createFile(tempDir.resolve("failureaccess.jar"));
        Path pomPath = Files.createFile(tempDir.resolve("failureaccess.pom"));
        Path repositoryLink = Files.createSymbolicLink(tempDir.resolve("failureaccess-1.0.3.jar"), installedJar);

        when(index.artifacts()).thenReturn(List.of(
                XmvnArtifact.builder("com.google.guava", "failureaccess", "1.0.3", tempDir.resolve("guava.xml"))
                        .path(installedJar)
                        .build()));
        when(index.pomOf(any())).thenReturn(Optional.of(pomPath));
        when(pomMetadataReader.read(pomPath)).thenReturn(new PomMetadata(
                null, null, List.of(new PomMetadataLicense("Apache-2.0", null))));

        List<SbomComponent> components = new DefaultSbomComponentCollector(pomMetadataReader, index)
                .collect(List.of(), List.of(), List.of(repositoryLink.toFile()));

        assertEquals(1, components.size());
        assertAll(
                () -> assertEquals(SbomComponentKind.LIBRARY, components.get(0).getComponentKind()),
                () -> assertEquals("com.google.guava:failureaccess:1.0.3", components.get(0).uniqueKey()),
                () -> assertEquals("Apache-2.0", components.get(0).getLicenses().get(0).getName())
        );
    }

    @Test
    @DisplayName("Skips ineligible coordinates and handles missing pom metadata")
    void skipsIneligibleCoordinatesAndHandlesMissingPomMetadata() {
        MavenCoordinate missingGroup = MavenCoordinate.builder()
                .artifactId("broken")
                .version("1.0.0")
                .packaging("jar")
                .build();
        MavenCoordinate noPomPath = coordinate(
                "org.example",
                "without-pom",
                "1.0.0",
                "jar",
                null
        );

        DefaultSbomComponentCollector collector = new DefaultSbomComponentCollector(pomMetadataReader, index);
        List<SbomComponent> components = collector.collect(
                List.of(missingGroup, noPomPath),
                null,
                null
        );

        assertAll(
                () -> assertEquals(1, components.size()),
                () -> assertEquals("without-pom", components.get(0).getArtifactId()),
                () -> assertTrue(components.get(0).getLicenses().isEmpty())
        );
        verifyNoInteractions(pomMetadataReader);
    }

    private MavenCoordinate coordinate(
            String groupId,
            String artifactId,
            String version,
            String packaging,
            Path pomPath
    ) {
        return MavenCoordinate.builder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .packaging(packaging)
                .pomPath(pomPath)
                .build();
    }
}
