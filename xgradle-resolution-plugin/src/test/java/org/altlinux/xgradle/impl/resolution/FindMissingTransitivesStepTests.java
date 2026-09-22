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
package org.altlinux.xgradle.impl.resolution;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.altlinux.xgradle.impl.metadata.MetadataModule;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;

import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("FindMissingTransitivesStep")
class FindMissingTransitivesStepTests {

    @Test
    @DisplayName("reports missing dependencies at any depth and ignores optional ones")
    void reportsMissingTransitives(@TempDir Path metadata) throws IOException {
        Files.writeString(metadata.resolve("packages.xml"), "<metadata><artifacts>"
                + artifact("app", dependency("lib", false) + dependency("absent", false) + dependency("extra", true))
                + artifact("lib", dependency("gone", false))
                + "</artifacts></metadata>");
        Injector injector = Guice.createInjector(new MetadataModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(Logger.class).toInstance(mock(Logger.class));
            }
        });
        injector.getInstance(MetadataIndex.class).build(List.of(metadata));

        ResolutionContext ctx = new ResolutionContext(mock(Gradle.class));
        ctx.putSystemArtifact("g:app", MavenCoordinate.builder().groupId("g").artifactId("app").version("1").build());
        injector.getInstance(FindMissingTransitivesStep.class).execute(ctx);

        assertEquals(Set.of("g:absent:1 (required by g:app)", "g:gone:1 (required by g:lib)"), ctx.getSkipped());
    }

    private static String artifact(String artifactId, String dependencies) {
        return "<artifact><groupId>g</groupId><artifactId>" + artifactId + "</artifactId><version>1</version>"
                + "<path>/usr/share/java/" + artifactId + ".jar</path>"
                + "<dependencies>" + dependencies + "</dependencies></artifact>";
    }

    private static String dependency(String artifactId, boolean optional) {
        return "<dependency><groupId>g</groupId><artifactId>" + artifactId + "</artifactId>"
                + "<requestedVersion>1</requestedVersion>"
                + (optional ? "<optional>true</optional>" : "") + "</dependency>";
    }
}
