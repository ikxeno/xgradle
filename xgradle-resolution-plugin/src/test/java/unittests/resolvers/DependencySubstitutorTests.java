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

import com.google.inject.Injector;

import org.altlinux.xgradle.impl.model.IvyRepository;
import unittests.metadata.Installations;
import org.altlinux.xgradle.impl.resolvers.DefaultDependencySubstitutor;
import org.altlinux.xgradle.interfaces.metadata.IvyRepositoryGenerator;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;

import org.gradle.api.Project;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.testfixtures.ProjectBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Resolves real requests through the generated repository with the substitutor applied.
 *
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("DependencySubstitutor contract")
class DependencySubstitutorTests {

    @TempDir
    Path temp;

    @Test
    @DisplayName("resolves declared, versionless, alias and compat requests to installed revisions")
    void resolvesToInstalledRevisions() throws IOException {
        Path metadata = Files.createDirectories(temp.resolve("metadata"));
        Path jars = Files.createDirectories(temp.resolve("java"));
        Files.writeString(metadata.resolve("packages.xml"), "<metadata><artifacts>"
                + artifact("lib", "1.5", jars.resolve("lib.jar"),
                "<aliases><alias><groupId>g</groupId><artifactId>lib-legacy</artifactId></alias></aliases>")
                + artifact("old", "2.0", jars.resolve("old.jar"), "<compatVersions><version>1.0</version></compatVersions>")
                + "</artifacts></metadata>");
        Files.writeString(jars.resolve("lib.jar"), "lib");
        Files.writeString(jars.resolve("old.jar"), "old");

        Injector injector = Installations.injector(List.of(metadata));
        MetadataIndex index = injector.getInstance(MetadataIndex.class);
        IvyRepository repository = injector.getInstance(IvyRepositoryGenerator.class).generate(temp.resolve("cache"));

        Project project = ProjectBuilder.builder().withProjectDir(temp.resolve("project").toFile()).build();
        project.getRepositories().ivy(repo -> {
            repo.setUrl(repository.getRoot().toUri());
            repo.patternLayout(layout -> {
                layout.ivy(IvyRepositoryGenerator.IVY_PATTERN);
                layout.artifact(IvyRepositoryGenerator.ARTIFACT_PATTERN);
            });
            repo.metadataSources(sources -> sources.ivyDescriptor());
        });
        project.getConfigurations().create("deps");
        List.of("g:lib:1.0", "g:lib", "g:lib-legacy:0.9", "g:old:1.0")
                .forEach(notation -> project.getDependencies().add("deps", notation));

        DefaultDependencySubstitutor substitutor = new DefaultDependencySubstitutor(index);
        substitutor.configure(project.getConfigurations());

        Set<String> resolved = project.getConfigurations().getByName("deps").getIncoming()
                .getResolutionResult().getAllComponents().stream()
                .map(ResolvedComponentResult::getModuleVersion)
                .filter(module -> module != null && module.getGroup().equals("g"))
                .map(module -> module.getName() + ":" + module.getVersion())
                .collect(Collectors.toSet());

        assertEquals(Set.of("lib:1.5", "lib-legacy:1.5", "old:1.0"), resolved);
        assertEquals(Optional.of("1.5"), substitutor.replacement("g", "lib", "1.0"));
        assertEquals(Optional.empty(), substitutor.replacement("g", "lib", "1.5"), "the installed version is kept");
    }

    private static String artifact(String artifactId, String version, Path path, String extra) {
        return "<artifact><groupId>g</groupId><artifactId>" + artifactId + "</artifactId>"
                + "<version>" + version + "</version><path>" + path + "</path>" + extra + "</artifact>";
    }
}
