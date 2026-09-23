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
import org.altlinux.xgradle.impl.model.XmvnDependency;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Renders an {@code ivy.xml} with a single {@code default} configuration.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class IvyDescriptor {

    private static final String CONF = "default";
    private static final String MAVEN_PREFIX = "m";
    private static final String MAVEN_NAMESPACE = "http://ant.apache.org/ivy/maven";

    private IvyDescriptor() {
    }

    /**
     * A module without a published artifact gets an empty {@code <publications/>},
     * because without it ivy expects a jar named after the module.
     *
     * @param publishedExtension extension of the module's main artifact, or null if it publishes none
     */
    static String render(String org, String name, String rev, String publishedExtension,
                         List<Dependency> dependencies) {
        IndentedXml xml = new IndentedXml();
        xml.start("ivy-module", "version", "2.0");
        xml.namespace(MAVEN_PREFIX, MAVEN_NAMESPACE);
        xml.empty("info", "organisation", org, "module", name, "revision", rev, "status", "release");

        xml.start("configurations");
        xml.empty("conf", "name", CONF);
        xml.end();

        if (publishedExtension != null) {
            xml.start("publications");
            xml.empty("artifact", "name", name, "type", publishedExtension, "ext", publishedExtension,
                    "conf", CONF);
            xml.end();
        } else {
            xml.empty("publications");
        }

        xml.start("dependencies");
        dependencies.forEach(dependency -> dependency(xml, dependency));
        xml.end();

        xml.end();
        return xml.finish();
    }

    private static void dependency(IndentedXml xml, Dependency resolved) {
        String[] attributes = {
                "org", resolved.org, "name", resolved.name, "rev", resolved.rev, "conf", CONF + "->" + CONF
        };
        XmvnDependency dep = resolved.metadata;
        boolean customArtifact = dep != null && !ArtifactKey.POM_EXTENSION.equals(dep.getExtension())
                && (!ArtifactKey.DEFAULT_EXTENSION.equals(dep.getExtension()) || !dep.getClassifier().isEmpty());
        boolean hasExclusions = dep != null && !dep.getExclusions().isEmpty();
        if (!customArtifact && !hasExclusions) {
            xml.empty("dependency", attributes);
            return;
        }

        xml.start("dependency", attributes);
        if (customArtifact) {
            xml.empty("artifact", "name", resolved.name, "type", dep.getExtension(), "ext", dep.getExtension());
            if (!dep.getClassifier().isEmpty()) {
                xml.attribute(MAVEN_PREFIX, MAVEN_NAMESPACE, "classifier", dep.getClassifier());
            }
        }
        dep.getExclusions().stream()
                .map(exclusion -> exclusion.split(":", 2))
                .forEach(ga -> xml.empty("exclude", "org", ga[0], "module", ga[1]));
        xml.end();
    }

    /** A dependency of a module on an installed module revision. */
    static final class Dependency {

        private final String org;
        private final String name;
        private final String rev;
        private final XmvnDependency metadata;

        /**
         * @param metadata the dependency as recorded in metadata, for its artifact and
         *                 exclusions; null for the dependency of an alias module
         */
        Dependency(String org, String name, String rev, XmvnDependency metadata) {
            this.org = org;
            this.name = name;
            this.rev = rev;
            this.metadata = metadata;
        }
    }

    /**
     * Writes one element per line, two spaces deeper for each level, the way ivy
     * files are usually laid out. Attributes are given as name and value pairs and
     * keep their order. The writer escapes the values.
     */
    private static final class IndentedXml {

        private final StringWriter out = new StringWriter();
        private final XMLStreamWriter writer;
        private int depth;

        IndentedXml() {
            try {
                writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
            } catch (XMLStreamException e) {
                throw new IllegalStateException("Cannot create an XML writer", e);
            }
            write(() -> writer.writeStartDocument("UTF-8", "1.0"));
        }

        void start(String element, String... attributes) {
            newLine();
            write(() -> writer.writeStartElement(element));
            attributes(attributes);
            depth++;
        }

        void empty(String element, String... attributes) {
            newLine();
            write(() -> writer.writeEmptyElement(element));
            attributes(attributes);
        }

        /** Declares a namespace on the element just started. */
        void namespace(String prefix, String uri) {
            write(() -> writer.writeNamespace(prefix, uri));
        }

        /** Adds a namespaced attribute to the element just written. */
        void attribute(String prefix, String uri, String name, String value) {
            write(() -> writer.writeAttribute(prefix, uri, name, value));
        }

        void end() {
            depth--;
            newLine();
            write(writer::writeEndElement);
        }

        String finish() {
            write(() -> {
                writer.writeCharacters("\n");
                writer.writeEndDocument();
                writer.close();
            });
            return out.toString();
        }

        private void attributes(String... attributes) {
            IntStream.range(0, attributes.length / 2)
                    .forEach(i -> write(() -> writer.writeAttribute(attributes[2 * i], attributes[2 * i + 1])));
        }

        private void newLine() {
            write(() -> writer.writeCharacters("\n" + "  ".repeat(depth)));
        }

        private static void write(XmlAction action) {
            try {
                action.run();
            } catch (XMLStreamException e) {
                throw new IllegalStateException("Cannot write ivy.xml", e);
            }
        }
    }

    private interface XmlAction {
        void run() throws XMLStreamException;
    }
}
