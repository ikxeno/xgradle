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

import com.google.inject.Inject;

import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.impl.model.XmvnDependency;
import org.altlinux.xgradle.interfaces.metadata.MetadataReader;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.altlinux.xgradle.impl.metadata.Xml.child;
import static org.altlinux.xgradle.impl.metadata.Xml.children;
import static org.altlinux.xgradle.impl.metadata.Xml.text;

/**
 * Reads XMvn metadata files. Like XMvn, it reads the files of a directory in name
 * order, accepts gzip-compressed files and skips a file it cannot read with a
 * warning, so one broken package does not break every build.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class DefaultMetadataReader implements MetadataReader {

    private final Logger logger;

    @Inject
    DefaultMetadataReader(Logger logger) {
        this.logger = logger;
    }

    @Override
    public List<XmvnArtifact> read(List<Path> locations) {
        return locations.stream()
                .flatMap(DefaultMetadataReader::metadataFiles)
                .flatMap(file -> readFile(file).stream())
                .collect(Collectors.toList());
    }

    private static Stream<Path> metadataFiles(Path location) {
        if (Files.isRegularFile(location)) {
            return Stream.of(location);
        }
        if (!Files.isDirectory(location)) {
            throw new GradleException("XMvn metadata location does not exist: " + location);
        }
        try (Stream<Path> entries = Files.list(location)) {
            return entries
                    .filter(Files::isRegularFile)
                    .sorted()
                    .collect(Collectors.toList())
                    .stream();
        } catch (IOException e) {
            throw new GradleException("Cannot list XMvn metadata directory " + location, e);
        }
    }

    private List<XmvnArtifact> readFile(Path file) {
        try (InputStream in = open(file)) {
            Document document = Xml.parse(in);
            return children(child(document.getDocumentElement(), "artifacts"), "artifact")
                    .map(artifact -> parseArtifact(artifact, file))
                    .collect(Collectors.toList());
        } catch (IOException | SAXException | InvalidMetadataException e) {
            logger.warn("Skipping XMvn metadata file {}: {}", file, e.getMessage());
            return List.of();
        }
    }

    /** Opens the file, unpacking it if it starts with the gzip magic; closes it if that fails. */
    private static InputStream open(Path file) throws IOException {
        BufferedInputStream in = new BufferedInputStream(Files.newInputStream(file));
        try {
            in.mark(2);
            int b1 = in.read();
            int b2 = in.read();
            in.reset();
            boolean gzip = b1 == (GZIPInputStream.GZIP_MAGIC & 0xff) && b2 == (GZIPInputStream.GZIP_MAGIC >> 8);
            return gzip ? new GZIPInputStream(in) : in;
        } catch (IOException e) {
            in.close();
            throw e;
        }
    }

    private static XmvnArtifact parseArtifact(Element e, Path file) {
        String path = text(e, "path");
        return XmvnArtifact.builder(
                        required(e, "groupId", file),
                        required(e, "artifactId", file),
                        required(e, "version", file),
                        file)
                .extension(text(e, "extension"))
                .classifier(text(e, "classifier"))
                .path(path == null ? null : Path.of(path))
                .namespace(text(e, "namespace"))
                .compatVersions(children(child(e, "compatVersions"), "version")
                        .map(Xml::text)
                        .collect(Collectors.toList()))
                .aliases(children(child(e, "aliases"), "alias")
                        .map(alias -> new ArtifactKey(
                                required(alias, "groupId", file),
                                required(alias, "artifactId", file),
                                text(alias, "extension"),
                                text(alias, "classifier"),
                                ArtifactKey.SYSTEM_VERSION))
                        .collect(Collectors.toList()))
                .dependencies(children(child(e, "dependencies"), "dependency")
                        .map(dependency -> parseDependency(dependency, file))
                        .collect(Collectors.toList()))
                .build();
    }

    private static XmvnDependency parseDependency(Element e, Path file) {
        return XmvnDependency.builder(required(e, "groupId", file), required(e, "artifactId", file))
                .extension(text(e, "extension"))
                .classifier(text(e, "classifier"))
                .requestedVersion(text(e, "requestedVersion"))
                .optional(Boolean.parseBoolean(text(e, "optional")))
                .exclusions(children(child(e, "exclusions"), "exclusion")
                        .map(x -> required(x, "groupId", file) + ":" + required(x, "artifactId", file))
                        .collect(Collectors.toList()))
                .build();
    }

    private static String required(Element e, String name, Path file) {
        String value = text(e, name);
        if (value == null || value.isEmpty()) {
            throw new InvalidMetadataException("<" + e.getLocalName() + "> without <" + name + ">");
        }
        return value;
    }

    private static final class InvalidMetadataException extends RuntimeException {

        private InvalidMetadataException(String message) {
            super(message);
        }
    }
}
