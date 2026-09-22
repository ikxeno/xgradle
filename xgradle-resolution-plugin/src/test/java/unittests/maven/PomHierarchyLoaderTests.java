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
package unittests.maven;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.altlinux.xgradle.impl.maven.MavenModule;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.maven.PomHierarchyLoader;
import org.apache.maven.model.Model;
import org.gradle.api.logging.Logger;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PomHierarchyLoader contract")
class PomHierarchyLoaderTests {

    @Mock
    private Logger logger;

    @Mock
    private PomFinder pomFinder;

    @Test
    @DisplayName("Loads parent-child hierarchy by artifactId.pom")
    void loadsParentHierarchy(@TempDir Path tempDir) throws Exception {
        Path parent = tempDir.resolve("parent.pom");
        Path child = tempDir.resolve("child.pom");

        Files.writeString(parent,
                "<project>\n" +
                        "  <modelVersion>4.0.0</modelVersion>\n" +
                        "  <groupId>g</groupId>\n" +
                        "  <artifactId>parent</artifactId>\n" +
                        "  <version>1</version>\n" +
                        "</project>\n");

        Files.writeString(child,
                "<project>\n" +
                        "  <modelVersion>4.0.0</modelVersion>\n" +
                        "  <parent>\n" +
                        "    <groupId>g</groupId>\n" +
                        "    <artifactId>parent</artifactId>\n" +
                        "    <version>1</version>\n" +
                        "  </parent>\n" +
                        "  <artifactId>child</artifactId>\n" +
                        "</project>\n");

        Injector injector = Guice.createInjector(
                Modules.override(new MavenModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Logger.class).toInstance(logger);
                        bind(MetadataIndex.class).toInstance(mock(MetadataIndex.class));
                        bind(PomFinder.class).toInstance(pomFinder);
                    }
                })
        );

        PomHierarchyLoader loader = injector.getInstance(PomHierarchyLoader.class);
        List<Model> hierarchy = loader.loadHierarchy(child);

        assertEquals(2, hierarchy.size());
        assertEquals("parent", hierarchy.get(0).getArtifactId());
        assertEquals("child", hierarchy.get(1).getArtifactId());
    }
}
