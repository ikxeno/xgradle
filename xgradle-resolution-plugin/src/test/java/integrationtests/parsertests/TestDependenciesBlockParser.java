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
package integrationtests.parsertests;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.impl.caches.CachesModule;
import org.altlinux.xgradle.impl.collectors.CollectorsModule;
import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.maven.MavenModule;
import org.altlinux.xgradle.impl.metadata.MetadataModule;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.parsers.ParsersModule;
import org.altlinux.xgradle.impl.utils.logging.LoggingModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Integration tests for {@link PomParser#parseDependencies(Path)}.
 *
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("Parse dependencies from POM files")
public class TestDependenciesBlockParser {
    private PomParser pomParser;
    private List<MavenCoordinate> parsedDeps;

    @BeforeEach
    public void prepareParser() {
        Injector injector = Guice.createInjector(
                new LoggingModule(),
                new CachesModule(),
                new CollectorsModule(),
                new MavenModule(),
                new MetadataModule(),
                new ParsersModule()
        );
        pomParser = injector.getInstance(PomParser.class);
    }

    @Test
    @DisplayName("Parses Maven Surefire dependency list")
    public void parseMavenSurefireCommonsPom (@TempDir Path tempDir) {
        preparePom("src/test/resources/poms/maven-surefire/maven-surefire-common.pom", tempDir);
        preparePom("src/test/resources/poms/maven-surefire/surefire.pom", tempDir);

        parsedDeps = pomParser.parseDependencies(tempDir.resolve(Path.of("maven-surefire-common.pom")));

        assertFalse(parsedDeps.isEmpty());

        assertEquals("apiguardian-api", parsedDeps.get(0).getArtifactId());
        assertEquals("surefire-api" ,parsedDeps.get(4).getArtifactId());
        assertEquals("plexus-xml", parsedDeps.get(parsedDeps.size() - 1).getArtifactId());

        assertTrue(checkDependencyScope(parsedDeps, "surefire-api", "compile"));
        assertTrue(checkDependencyScope(parsedDeps, "maven-core", "provided"));
        assertTrue(checkDependencyScope(parsedDeps, "powermock-module-junit4", "test"));

        assertTrue(isDependencyContained(parsedDeps, "org.codehaus.plexus", "plexus-java"));
        assertTrue(isDependencyContained(parsedDeps, "org.mockito", "mockito-core"));
        assertTrue(isDependencyContained(parsedDeps, "org.apache.maven.surefire", "surefire-shared-utils"));

        assertTrue(checkDependencyVersion(parsedDeps, "surefire-booter", "3.2.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-common-artifact-filters", "3.1.1"));
        assertTrue(checkDependencyVersion(parsedDeps, "jansi", "2.4.0"));

        assertEquals(20, parsedDeps.size());
    }

    @Test
    @DisplayName("Parses Plexus XML dependency list")
    public void parsePlexusXmlPom (@TempDir Path tempDir) {
        preparePom("src/test/resources/poms/plexus-xml/plexus-xml.pom", tempDir);

        parsedDeps = pomParser.parseDependencies(tempDir.resolve(Path.of("plexus-xml.pom")));

        assertFalse(parsedDeps.isEmpty());

        assertEquals("plexus-utils", parsedDeps.get(0).getArtifactId());
        assertEquals("jmh-core", parsedDeps.get(1).getArtifactId());
        assertEquals("jmh-generator-annprocess", parsedDeps.get(2).getArtifactId());
        assertEquals("junit", parsedDeps.get(parsedDeps.size() - 1).getArtifactId());

        assertTrue(checkDependencyScope(parsedDeps, "plexus-utils", "test"));
        assertTrue(checkDependencyScope(parsedDeps, "jmh-core", "test"));
        assertTrue(checkDependencyScope(parsedDeps, "jmh-generator-annprocess", "test"));
        assertTrue(checkDependencyScope(parsedDeps, "junit", "test"));

        assertTrue(checkDependencyVersion(parsedDeps, "plexus-utils", "4.0.0"));
        assertTrue(checkDependencyVersion(parsedDeps, "jmh-core", "1.36"));
        assertTrue(checkDependencyVersion(parsedDeps, "jmh-generator-annprocess", "1.36"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit", "4.13.2"));

        assertEquals(4, parsedDeps.size());
    }

    @Test
    @DisplayName("Parses JUnit Jupiter dependency list")
    public void parseJunitJupiterPom (@TempDir Path tempDir) {
        preparePom("src/test/resources/poms/junit5/junit-jupiter.pom", tempDir);

        parsedDeps = pomParser.parseDependencies(tempDir.resolve(Path.of("junit-jupiter.pom")));

        assertFalse(parsedDeps.isEmpty());

        assertEquals("junit-jupiter-api", parsedDeps.get(0).getArtifactId());
        assertEquals("junit-jupiter-params", parsedDeps.get(1).getArtifactId());
        assertEquals("junit-jupiter-engine", parsedDeps.get(parsedDeps.size() - 1).getArtifactId());

        assertTrue(checkDependencyScope(parsedDeps, "junit-jupiter-api", "compile"));
        assertTrue(checkDependencyScope(parsedDeps, "junit-jupiter-params", "compile"));
        assertTrue(checkDependencyScope(parsedDeps, "junit-jupiter-engine", "compile"));

        assertTrue(checkDependencyVersion(parsedDeps, "junit-jupiter-api", "5.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-jupiter-params", "5.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-jupiter-engine", "5.8.2"));

        assertEquals(3, parsedDeps.size());
    }

    @Test
    @DisplayName("Parses ASM Commons dependency list")
    public void parseAsmCommonsPom(@TempDir Path tempDir) {
        preparePom("src/test/resources/poms/asm/asm-commons.pom", tempDir);

        parsedDeps = pomParser.parseDependencies(tempDir.resolve(Path.of("asm-commons.pom")));

        assertFalse(parsedDeps.isEmpty());

        assertEquals("asm", parsedDeps.get(0).getArtifactId());
        assertEquals("asm-tree", parsedDeps.get(parsedDeps.size() - 1).getArtifactId());

        assertTrue(checkDependencyScope(parsedDeps, "asm", "compile"));
        assertTrue(checkDependencyScope(parsedDeps, "asm-tree", "compile"));

        assertTrue(checkDependencyVersion(parsedDeps, "asm", "9.8"));
        assertTrue(checkDependencyVersion(parsedDeps, "asm-tree", "9.8"));

        assertEquals(2, parsedDeps.size());
    }

    @Test
    @DisplayName("Parses Maven Wagon HTTP dependency list")
    public void parseMavenWagonHttpPom(@TempDir Path tempDir) {
        preparePom("src/test/resources/poms/maven-wagon/wagon-providers.pom", tempDir);
        preparePom("src/test/resources/poms/maven-wagon/wagon-http.pom", tempDir);
        preparePom("src/test/resources/poms/maven-wagon/wagon.pom", tempDir);

        parsedDeps = pomParser.parseDependencies(tempDir.resolve(Path.of("wagon-http.pom")));

        assertFalse(parsedDeps.isEmpty());

        assertEquals("wagon-provider-api", parsedDeps.get(0).getArtifactId());
        assertEquals("wagon-http-shared", parsedDeps.get(1).getArtifactId());
        assertEquals("javax.servlet-api", parsedDeps.get(parsedDeps.size() - 2).getArtifactId());
        assertEquals("plexus-container-default", parsedDeps.get(parsedDeps.size() - 1).getArtifactId());

        assertTrue(checkDependencyScope(parsedDeps, "wagon-http-shared", "compile"));
        assertTrue(checkDependencyScope(parsedDeps, "httpclient", "compile"));
        assertTrue(checkDependencyScope(parsedDeps, "jcl-over-slf4j", "runtime"));
        assertTrue(checkDependencyScope(parsedDeps, "slf4j-api", "test"));

        assertTrue(checkDependencyVersion(parsedDeps, "wagon-http-shared", "3.5.3"));
        assertTrue(checkDependencyVersion(parsedDeps, "jcl-over-slf4j", "1.7.36"));
        assertTrue(checkDependencyVersion(parsedDeps, "jetty-all", "9.2.30.v20200428"));
        assertTrue(checkDependencyVersion(parsedDeps, "plexus-container-default", "2.1.0"));

        assertEquals(10 ,parsedDeps.size());
    }

    private boolean checkDependencyScope(List<MavenCoordinate> parsedDeps, String artifactId, String scope) {
        MavenCoordinate dependency = findDependency(parsedDeps, artifactId);
        if (dependency == null) {
            return false;
        }
        return dependency.getScope() == MavenScope.fromScope(scope);
    }

    private boolean checkDependencyVersion(List<MavenCoordinate> parsedDeps, String artifactId, String version) {
        return findDependency(parsedDeps, artifactId).getVersion().equals(version);
    }

    private boolean isDependencyContained(List<MavenCoordinate> parsedDeps, String groupId, String artifactId) {
        return parsedDeps.stream()
                .anyMatch(dep -> dep.getArtifactId().equals(artifactId) && dep.getGroupId().equals(groupId));
    }

    private MavenCoordinate findDependency(List<MavenCoordinate> parsedDeps, String artifactId) {
        return parsedDeps.stream()
                .filter(dep -> dep.getArtifactId().equals(artifactId))
                .findFirst()
                .orElse(null);
    }

    private void preparePom(String sourcePom, Path targetDir) {
        try {
            Path pathToPom = Path.of(sourcePom);
            Path target = targetDir.resolve(pathToPom.getFileName());
            Files.copy(pathToPom, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
