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

import org.gradle.api.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A directory of repositories named by the fingerprint of their content and
 * shared by concurrent builds. A repository is written into a temporary
 * directory that is then renamed, so no build sees one half written, and
 * repositories no build used for a week are removed.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class RepositoryCache {

    private static final String COMPLETE_MARKER = ".complete";
    private static final Duration UNUSED_FOR = Duration.ofDays(7);

    /** Writes repository content into a directory. */
    interface ContentWriter {
        void writeTo(Path directory) throws IOException;
    }

    private final Path directory;
    private final Logger logger;

    RepositoryCache(Path directory, Logger logger) {
        this.directory = directory;
        this.logger = logger;
    }

    /**
     * The repository with the given fingerprint, written first if no build has written it yet.
     */
    Path obtain(String fingerprint, ContentWriter writer) throws IOException {
        Path root = directory.resolve(fingerprint);
        if (isComplete(root)) {
            Files.setLastModifiedTime(root.resolve(COMPLETE_MARKER), FileTime.from(Instant.now()));
        } else {
            write(root, writer);
        }
        removeUnused(root);
        return root;
    }

    private void write(Path root, ContentWriter writer) throws IOException {
        Files.createDirectories(directory);
        Path tmp = Files.createTempDirectory(directory, root.getFileName() + ".");
        try {
            writer.writeTo(tmp);
            Files.createFile(tmp.resolve(COMPLETE_MARKER));
            moveIntoPlace(tmp, root);
        } finally {
            if (Files.exists(tmp)) {
                deleteRecursively(tmp);
            }
        }
    }

    /**
     * Removes repositories no build has used for {@link #UNUSED_FOR}, and
     * temporary directories of builds that died that long ago. A build marks
     * its repository as used by touching the complete marker.
     */
    private void removeUnused(Path current) {
        Instant cutoff = Instant.now().minus(UNUSED_FOR);
        try (Stream<Path> dirs = Files.list(directory)) {
            dirs.filter(dir -> !dir.equals(current))
                    .filter(dir -> lastUsed(dir).isBefore(cutoff))
                    .collect(Collectors.toList())
                    .forEach(this::removeQuietly);
        } catch (IOException e) {
            logger.warn("Cannot clean up old system ivy repositories in {}: {}", directory, e.toString());
        }
    }

    private static Instant lastUsed(Path dir) {
        Path marker = dir.resolve(COMPLETE_MARKER);
        try {
            return Files.getLastModifiedTime(Files.exists(marker) ? marker : dir).toInstant();
        } catch (IOException e) {
            return Instant.now();
        }
    }

    private void removeQuietly(Path dir) {
        try {
            discard(dir);
            logger.info("Removed unused system ivy repository {}", dir);
        } catch (IOException e) {
            logger.warn("Cannot remove unused system ivy repository {}: {}", dir, e.toString());
        }
    }

    /**
     * Renames the written repository to its final name. Another build may have
     * renamed its copy first; depending on the platform the rename then fails with
     * {@code FileAlreadyExistsException}, {@code DirectoryNotEmptyException} or a
     * plain {@code FileSystemException}, so the outcome is judged by the marker.
     * A directory without the marker is a leftover and is replaced.
     */
    private void moveIntoPlace(Path tmp, Path root) throws IOException {
        try {
            Files.move(tmp, root, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            if (isComplete(root)) {
                logger.info("System ivy repository {} was generated concurrently", root);
                return;
            }
            logger.warn("Replacing incomplete system ivy repository {}", root);
            discard(root);
            try {
                Files.move(tmp, root, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException again) {
                if (!isComplete(root)) {
                    again.addSuppressed(e);
                    throw again;
                }
            }
        }
    }

    private static boolean isComplete(Path root) {
        return Files.isRegularFile(root.resolve(COMPLETE_MARKER));
    }

    /** Renames a directory out of the way before deleting it, so no one sees it half deleted. */
    private static void discard(Path dir) throws IOException {
        Path trash = Files.createTempDirectory(dir.getParent(), dir.getFileName() + ".old.");
        try {
            Files.move(dir, trash.resolve("content"), StandardCopyOption.ATOMIC_MOVE);
        } catch (NoSuchFileException e) {
            // Discarded by another build already.
        }
        deleteRecursively(trash);
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.delete(path);
            }
        }
    }
}
