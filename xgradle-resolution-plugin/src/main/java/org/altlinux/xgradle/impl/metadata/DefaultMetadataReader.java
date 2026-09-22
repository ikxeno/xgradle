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

import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.impl.model.XmvnDependency;
import org.altlinux.xgradle.interfaces.metadata.MetadataReader;

import org.gradle.api.GradleException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Reads XMvn metadata files, following the XMvn reader: files in a directory
 * are read in name order and may be gzip-compressed. Unlike XMvn, a file that
 * cannot be read fails the build instead of being skipped, since a skipped file
 * silently changes the resolved classpath.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class DefaultMetadataReader implements MetadataReader {

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

    private static List<XmvnArtifact> readFile(Path file) {
        Document document;
        try (InputStream in = open(file)) {
            document = newDocumentBuilder().parse(in);
        } catch (IOException | SAXException e) {
            throw new GradleException("Cannot read XMvn metadata file " + file + ": " + e.getMessage(), e);
        }

        return children(child(document.getDocumentElement(), "artifacts"), "artifact")
                .map(artifact -> parseArtifact(artifact, file))
                .collect(Collectors.toList());
    }

    private static InputStream open(Path file) throws IOException {
        BufferedInputStream in = new BufferedInputStream(Files.newInputStream(file));
        in.mark(2);
        int b1 = in.read();
        int b2 = in.read();
        in.reset();
        boolean gzip = b1 == (GZIPInputStream.GZIP_MAGIC & 0xff) && b2 == (GZIPInputStream.GZIP_MAGIC >> 8);
        return gzip ? new GZIPInputStream(in) : in;
    }

    private static DocumentBuilder newDocumentBuilder() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new GradleException("Cannot create XML parser for XMvn metadata", e);
        }
    }

    private static XmvnArtifact parseArtifact(Element e, Path file) {
        String groupId = required(e, "groupId", file);
        String artifactId = required(e, "artifactId", file);
        String version = required(e, "version", file);
        String path = text(e, "path");

        List<String> compatVersions = children(child(e, "compatVersions"), "version")
                .map(DefaultMetadataReader::text)
                .collect(Collectors.toList());

        List<ArtifactKey> aliases = children(child(e, "aliases"), "alias")
                .map(alias -> new ArtifactKey(
                        required(alias, "groupId", file),
                        required(alias, "artifactId", file),
                        text(alias, "extension"),
                        text(alias, "classifier"),
                        ArtifactKey.SYSTEM_VERSION))
                .collect(Collectors.toList());

        List<XmvnDependency> dependencies = children(child(e, "dependencies"), "dependency")
                .map(dep -> parseDependency(dep, file))
                .collect(Collectors.toList());

        return new XmvnArtifact(
                groupId,
                artifactId,
                text(e, "extension"),
                text(e, "classifier"),
                version,
                path == null ? null : Path.of(path),
                text(e, "namespace"),
                properties(child(e, "properties")),
                compatVersions,
                aliases,
                dependencies,
                file);
    }

    private static XmvnDependency parseDependency(Element e, Path file) {
        List<String> exclusions = children(child(e, "exclusions"), "exclusion")
                .map(x -> required(x, "groupId", file) + ":" + required(x, "artifactId", file))
                .collect(Collectors.toList());
        return new XmvnDependency(
                required(e, "groupId", file),
                required(e, "artifactId", file),
                text(e, "extension"),
                text(e, "classifier"),
                text(e, "requestedVersion"),
                text(e, "resolvedVersion"),
                text(e, "namespace"),
                Boolean.parseBoolean(text(e, "optional")),
                exclusions);
    }

    private static Map<String, String> properties(Element e) {
        return children(e, null).collect(Collectors.toMap(
                Element::getLocalName,
                DefaultMetadataReader::text,
                (first, second) -> second,
                LinkedHashMap::new));
    }

    private static String required(Element e, String name, Path file) {
        String value = text(e, name);
        if (value == null || value.isEmpty()) {
            throw new GradleException("XMvn metadata file " + file + " has an <" + e.getLocalName()
                    + "> without <" + name + ">");
        }
        return value;
    }

    private static String text(Element e, String name) {
        Element c = child(e, name);
        return c == null ? null : text(c);
    }

    private static String text(Element e) {
        return e.getTextContent().trim();
    }

    private static Element child(Element parent, String name) {
        return children(parent, name).findFirst().orElse(null);
    }

    /**
     * Direct child elements with the given local name, or all of them if name is null.
     */
    private static Stream<Element> children(Element parent, String name) {
        if (parent == null) {
            return Stream.empty();
        }
        return Stream.iterate(parent.getFirstChild(), Objects::nonNull, Node::getNextSibling)
                .filter(n -> n.getNodeType() == Node.ELEMENT_NODE)
                .filter(n -> name == null || name.equals(n.getLocalName()))
                .map(Element.class::cast);
    }
}
