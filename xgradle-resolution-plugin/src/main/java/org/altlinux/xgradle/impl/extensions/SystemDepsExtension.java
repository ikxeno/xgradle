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
package org.altlinux.xgradle.impl.extensions;

import org.altlinux.xgradle.impl.metadata.XmvnConfiguration;
import org.altlinux.xgradle.impl.utils.config.XGradleConfig;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Provides access to system-level dependency paths for the plugin.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public class SystemDepsExtension {

    private static final String MAVEN_METADATA_DIR_KEY = "maven.metadata.dir";
    private static final Path DEFAULT_METADATA_DIR = Path.of("/usr/share/maven-metadata");
    private static final String PATH_SEPARATOR = ",";
    private static final Logger LOGGER = Logging.getLogger(SystemDepsExtension.class);
    private static final AtomicBoolean MISSING_METADATA_PATH_LOGGED = new AtomicBoolean(false);

    /**
     * XMvn metadata locations: {@code maven.metadata.dir} (comma-separated) from a
     * system property or {@code ~/.xgradle/xgradle.config}; otherwise the metadata
     * repositories of the XMvn configuration that exist, as XMvn skips the others;
     * otherwise {@code /usr/share/maven-metadata} if it exists. A location set
     * explicitly is returned even if it is missing, so that reading it fails loudly.
     */
    public static List<Path> getMetadataPaths(XmvnConfiguration xmvn) {
        Optional<String> configured = Optional.ofNullable(System.getProperty(MAVEN_METADATA_DIR_KEY))
                .or(() -> Optional.ofNullable(XGradleConfig.getConfigProperty(MAVEN_METADATA_DIR_KEY)))
                .filter(value -> !value.isBlank());
        if (configured.isPresent()) {
            return Arrays.stream(configured.get().split(PATH_SEPARATOR))
                    .map(String::trim)
                    .filter(part -> !part.isEmpty())
                    .distinct()
                    .map(Path::of)
                    .collect(Collectors.toList());
        }
        List<Path> fromXmvn = xmvn.getMetadataRepositories().stream()
                .filter(Files::exists)
                .collect(Collectors.toList());
        if (!fromXmvn.isEmpty()) {
            return fromXmvn;
        }
        if (Files.isDirectory(DEFAULT_METADATA_DIR)) {
            return List.of(DEFAULT_METADATA_DIR);
        }
        if (MISSING_METADATA_PATH_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("No XMvn metadata found: {} does not exist and -D{} is not set",
                    DEFAULT_METADATA_DIR, MAVEN_METADATA_DIR_KEY);
        }
        return List.of();
    }
}
