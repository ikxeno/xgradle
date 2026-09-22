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
import org.altlinux.xgradle.impl.metadata.MetadataModule;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
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

    @Test
    @DisplayName("Loads the installed compat version of the parent a POM names")
    void loadsCompatParent(@TempDir Path tempDir) throws Exception {
        Path metadata = Files.createDirectories(tempDir.resolve("metadata"));
        Files.writeString(tempDir.resolve("parent-2.pom"), pom("parent", "2", ""));
        Files.writeString(tempDir.resolve("parent-1.pom"), pom("parent", "1", ""));
        Path child = tempDir.resolve("child.pom");
        Files.writeString(child, pom("child", "1",
                "<parent><groupId>g</groupId><artifactId>parent</artifactId><version>1</version></parent>"));
        Files.writeString(metadata.resolve("parent.xml"), "<metadata><artifacts>"
                + "<artifact><groupId>g</groupId><artifactId>parent</artifactId><extension>pom</extension>"
                + "<version>2</version><path>" + tempDir.resolve("parent-2.pom") + "</path></artifact>"
                + "<artifact><groupId>g</groupId><artifactId>parent</artifactId><extension>pom</extension>"
                + "<version>1</version><path>" + tempDir.resolve("parent-1.pom") + "</path>"
                + "<compatVersions><version>1</version></compatVersions></artifact>"
                + "</artifacts></metadata>");

        Injector injector = Guice.createInjector(new MavenModule(), new MetadataModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(Logger.class).toInstance(logger);
                bind(PomParser.class).toInstance(mock(PomParser.class));
            }
        });
        injector.getInstance(MetadataIndex.class).build(List.of(metadata));

        List<Model> hierarchy = injector.getInstance(PomHierarchyLoader.class).loadHierarchy(child);

        assertEquals("1", hierarchy.get(0).getVersion());
    }

    private static String pom(String artifactId, String version, String parent) {
        return "<project><modelVersion>4.0.0</modelVersion>" + parent
                + "<groupId>g</groupId><artifactId>" + artifactId + "</artifactId>"
                + "<version>" + version + "</version></project>";
    }
}
