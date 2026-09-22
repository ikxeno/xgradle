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
package unittests.parsers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.parsers.ParsersModule;
import org.altlinux.xgradle.interfaces.maven.PomHierarchyLoader;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PomParser contract")
class PomParserTests {

    @Mock
    private PomHierarchyLoader loader;


    @Test
    @DisplayName("Reads coordinates from the POM hierarchy")
    void readsCoordinates() {
        Path pomPath = Path.of("file.pom");

        Model model = new Model();
        model.setGroupId("g");
        model.setArtifactId("a");
        model.setVersion("1");
        when(loader.loadHierarchy(pomPath)).thenReturn(List.of(model));

        Injector injector = Guice.createInjector(
                Modules.override(new ParsersModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomHierarchyLoader.class).toInstance(loader);
                    }
                })
        );

        PomParser parser = injector.getInstance(PomParser.class);
        MavenCoordinate result = parser.parsePom(pomPath);

        assertNotNull(result);
        assertEquals("g", result.getGroupId());
        assertEquals("a", result.getArtifactId());
        assertEquals("1", result.getVersion());
    }
}
