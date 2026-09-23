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

import org.gradle.api.GradleException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Namespace-agnostic reading of XMvn XML files, which come with and without
 * their namespace, through a parser that refuses DTDs and external entities.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class Xml {

    private Xml() {
    }

    static Document parse(InputStream in) throws IOException, SAXException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(in);
        } catch (ParserConfigurationException e) {
            throw new GradleException("Cannot create a secure XML parser", e);
        }
    }

    /** Trimmed text of the first child with the given local name, or null without one. */
    static String text(Element parent, String name) {
        Element child = child(parent, name);
        return child == null ? null : text(child);
    }

    static String text(Element element) {
        return element.getTextContent().trim();
    }

    static Element child(Element parent, String name) {
        return children(parent, name).findFirst().orElse(null);
    }

    /**
     * Direct child elements with the given local name, or all of them if name is null.
     */
    static Stream<Element> children(Element parent, String name) {
        if (parent == null) {
            return Stream.empty();
        }
        return Stream.iterate(parent.getFirstChild(), Objects::nonNull, Node::getNextSibling)
                .filter(n -> n.getNodeType() == Node.ELEMENT_NODE)
                .filter(n -> name == null || name.equals(n.getLocalName()))
                .map(Element.class::cast);
    }
}
