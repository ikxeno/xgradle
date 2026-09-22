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
package unittests.resolvers;

import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.impl.resolvers.DefaultArtifactResolver;
import org.gradle.api.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArtifactResolver contract")
class ArtifactResolverTests {

    @Mock
    private PomFinder pomFinder;

    @Mock
    private Logger logger;

    @Test
    @DisplayName("Looks declared dependencies up in the index and filter drops BOMs")
    void resolvesAndFilters() {
        DefaultArtifactResolver resolver = new DefaultArtifactResolver(pomFinder);
        MavenCoordinate lib = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("lib")
                .version("1")
                .build();
        MavenCoordinate bom = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("bom")
                .version("1")
                .packaging("pom")
                .build();
        when(pomFinder.findPomForArtifact("g", "lib")).thenReturn(lib);
        when(pomFinder.findPomForArtifact("g", "bom")).thenReturn(bom);

        resolver.resolve(Set.of("g:lib", "g:bom", "g:missing"), logger);
        resolver.filter();

        assertEquals(Map.of("g:lib", lib), resolver.getSystemArtifacts());
        assertEquals(Set.of("g:missing"), resolver.getNotFoundDependencies());
    }
}
