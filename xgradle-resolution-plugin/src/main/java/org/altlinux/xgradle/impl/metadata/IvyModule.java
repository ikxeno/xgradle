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
package org.altlinux.xgradle.impl.metadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * One module of the generated repository: its directory, its {@code ivy.xml} and
 * the symlinks to its installed files.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class IvyModule {

    private final String directory;
    private final String descriptor;
    private final SortedMap<String, Path> files;

    /**
     * @param directory  {@code org/name/rev} inside the repository
     * @param descriptor the {@code ivy.xml} content
     * @param files      link names to the installed files they point at
     */
    IvyModule(String directory, String descriptor, Map<String, Path> files) {
        this.directory = directory;
        this.descriptor = descriptor;
        this.files = new TreeMap<>(files);
    }

    void writeTo(Path repository) throws IOException {
        Path dir = repository.resolve(directory);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("ivy.xml"), descriptor, StandardCharsets.UTF_8);
        for (Map.Entry<String, Path> file : files.entrySet()) {
            Files.createSymbolicLink(dir.resolve(file.getKey()), file.getValue());
        }
    }

    /** Everything the module writes, line by line, for the repository fingerprint. */
    Stream<String> contentLines() {
        return Stream.concat(
                Stream.of(directory, descriptor),
                files.entrySet().stream().map(file -> file.getKey() + " -> " + file.getValue()));
    }
}
