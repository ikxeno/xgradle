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
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Directory of repositories named by the fingerprint of their content and shared
 * by concurrent builds. A repository is written to a temporary directory and then
 * renamed, so no build sees it half written. Repositories unused for a week are removed.
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
     * The repository with the given fingerprint, written first if no build has written it
     * yet or another build has just removed it as unused.
     */
    Path obtain(String fingerprint, ContentWriter writer) throws IOException {
        Path root = directory.resolve(fingerprint);
        if (!markUsed(root)) {
            write(root, writer);
        }
        removeUnused(root);
        return root;
    }

    /**
     * Touches the complete marker. Touching instead of checking first leaves no gap in
     * which a cleanup could remove the repository after it was found.
     */
    private static boolean markUsed(Path root) throws IOException {
        try {
            Files.setLastModifiedTime(root.resolve(COMPLETE_MARKER), FileTime.from(Instant.now()));
            return true;
        } catch (NoSuchFileException e) {
            return false;
        }
    }

    private void write(Path root, ContentWriter writer) throws IOException {
        Files.createDirectories(directory);
        Path tmp = Files.createTempDirectory(directory, root.getFileName() + ".");
        try {
            makeReadableByAll(tmp);
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
     * A temporary directory is created private to its owner; the repository it becomes
     * must stay readable to every user of a shared Gradle user home.
     * The attribute view is asked for directly: looking up the file store needs a
     * mount table, and a hasher chroot has none for its /tmp.
     */
    private static void makeReadableByAll(Path dir) throws IOException {
        PosixFileAttributeView view = Files.getFileAttributeView(dir, PosixFileAttributeView.class);
        if (view != null) {
            view.setPermissions(PosixFilePermissions.fromString("rwxr-xr-x"));
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
                    .forEach(dir -> removeQuietly(dir, cutoff));
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

    /**
     * Checks the last use again right before removing, since another build may have
     * started using the repository after the directory was listed.
     */
    private void removeQuietly(Path dir, Instant cutoff) {
        if (!lastUsed(dir).isBefore(cutoff)) {
            return;
        }
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
     * plain {@code FileSystemException}, so the marker decides whether the repository is in place.
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

    /**
     * Renames a directory to a temporary name, then deletes it, so other builds never
     * see it half deleted. A directory another build has already removed is skipped.
     */
    private static void discard(Path dir) throws IOException {
        Path trash = Files.createTempDirectory(dir.getParent(), dir.getFileName() + ".old.");
        try {
            Files.move(dir, trash.resolve("content"), StandardCopyOption.ATOMIC_MOVE);
        } catch (NoSuchFileException ignored) {
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
