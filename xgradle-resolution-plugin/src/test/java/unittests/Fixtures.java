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
package unittests;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * XML files for tests, kept under {@code src/test/resources/fixtures}. A fixture is
 * a single file or a directory of files, copied into a test directory. Paths to
 * installed files are only known when a test runs, so a fixture writes the directory
 * that holds them as {@code @DIR@}.
 *
 * @author Ivan Khanas xeno@altlinux.org
 */
public final class Fixtures {

    private static final String ROOT = "/fixtures/";
    private static final String DIR = "@DIR@";

    private Fixtures() {
    }

    /** Copies a fixture into {@code target}, a directory created if needed. */
    public static Path copy(String fixture, Path target) {
        return copy(fixture, target, target);
    }

    /**
     * Copies a fixture into {@code target}, a directory created if needed, with
     * {@code @DIR@} replaced by {@code dir}.
     */
    public static Path copy(String fixture, Path target, Path dir) {
        Path source = locate(fixture);
        try {
            Files.createDirectories(target);
            if (Files.isDirectory(source)) {
                try (Stream<Path> files = Files.list(source)) {
                    files.collect(Collectors.toList()).forEach(file -> write(file, target, dir));
                }
            } else {
                write(source, target, dir);
            }
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path file, Path target, Path dir) {
        try {
            String content = Files.readString(file).replace(DIR, dir.toString());
            Files.writeString(target.resolve(file.getFileName().toString()), content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path locate(String fixture) {
        URL url = Objects.requireNonNull(Fixtures.class.getResource(ROOT + fixture), "no fixture " + fixture);
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
