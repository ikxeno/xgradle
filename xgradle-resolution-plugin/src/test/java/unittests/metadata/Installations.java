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
package unittests.metadata;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.altlinux.xgradle.impl.metadata.MetadataModule;
import org.altlinux.xgradle.impl.model.InstalledLayout;
import org.altlinux.xgradle.interfaces.parsers.PomParser;

import org.gradle.api.logging.Logger;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Injectors over an installation described only by XMvn metadata.
 *
 * @author Ivan Khanas xeno@altlinux.org
 */
public final class Installations {

    private static final Path NOTHING_INSTALLED = Path.of("/nonexistent");

    private Installations() {
    }

    public static InstalledLayout metadataOnly(List<Path> metadataLocations, boolean ignoreDuplicates) {
        return new InstalledLayout(metadataLocations, ignoreDuplicates, NOTHING_INSTALLED, NOTHING_INSTALLED);
    }

    public static Injector injector(List<Path> metadataLocations) {
        return injector(metadataOnly(metadataLocations, true), mock(Logger.class));
    }

    public static Injector injector(InstalledLayout layout, Logger logger) {
        return Guice.createInjector(new MetadataModule(layout), new AbstractModule() {
            @Override
            protected void configure() {
                bind(Logger.class).toInstance(logger);
                bind(PomParser.class).toInstance(mock(PomParser.class));
            }
        });
    }
}
