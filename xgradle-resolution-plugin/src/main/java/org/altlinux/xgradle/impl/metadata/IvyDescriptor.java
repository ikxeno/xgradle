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

import java.util.List;
import java.util.Optional;

/**
 * Renders an {@code ivy.xml} with a single {@code default} configuration.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class IvyDescriptor {

    private static final String CONF = "default";

    private IvyDescriptor() {
    }

    /**
     * A module without a published artifact gets an empty {@code <publications/>},
     * because without it ivy expects a jar named after the module.
     *
     * @param publishedExtension extension of the module's main artifact, if it has one
     */
    static String render(String org, String name, String rev, Optional<String> publishedExtension,
                         List<Dependency> dependencies) {
        StringBuilder xml = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<ivy-module version=\"2.0\" xmlns:m=\"http://ant.apache.org/ivy/maven\">\n")
                .append("  <info organisation=\"").append(escape(org))
                .append("\" module=\"").append(escape(name))
                .append("\" revision=\"").append(escape(rev))
                .append("\" status=\"release\"/>\n")
                .append("  <configurations>\n    <conf name=\"").append(CONF).append("\"/>\n  </configurations>\n");

        if (publishedExtension.isPresent()) {
            String ext = escape(publishedExtension.get());
            xml.append("  <publications>\n    <artifact name=\"").append(escape(name))
                    .append("\" type=\"").append(ext).append("\" ext=\"").append(ext)
                    .append("\" conf=\"").append(CONF).append("\"/>\n  </publications>\n");
        } else {
            xml.append("  <publications/>\n");
        }

        xml.append("  <dependencies>\n");
        dependencies.forEach(dependency -> xml.append(dependency(dependency)));
        return xml.append("  </dependencies>\n</ivy-module>\n").toString();
    }

    private static String dependency(Dependency resolved) {
        StringBuilder xml = new StringBuilder("    <dependency org=\"").append(escape(resolved.org))
                .append("\" name=\"").append(escape(resolved.name))
                .append("\" rev=\"").append(escape(resolved.rev))
                .append("\" conf=\"").append(CONF).append("->").append(CONF).append("\"");

        XmvnDependency dep = resolved.metadata;
        boolean customArtifact = dep != null && !ArtifactKey.POM_EXTENSION.equals(dep.getExtension())
                && (!ArtifactKey.DEFAULT_EXTENSION.equals(dep.getExtension()) || !dep.getClassifier().isEmpty());
        boolean hasExclusions = dep != null && !dep.getExclusions().isEmpty();
        if (!customArtifact && !hasExclusions) {
            return xml.append("/>\n").toString();
        }

        xml.append(">\n");
        if (customArtifact) {
            String ext = escape(dep.getExtension());
            xml.append("      <artifact name=\"").append(escape(resolved.name))
                    .append("\" type=\"").append(ext).append("\" ext=\"").append(ext).append("\"");
            if (!dep.getClassifier().isEmpty()) {
                xml.append(" m:classifier=\"").append(escape(dep.getClassifier())).append("\"");
            }
            xml.append("/>\n");
        }
        if (hasExclusions) {
            dep.getExclusions().stream()
                    .map(exclusion -> exclusion.split(":", 2))
                    .forEach(ga -> xml.append("      <exclude org=\"").append(escape(ga[0]))
                            .append("\" module=\"").append(escape(ga[1])).append("\"/>\n"));
        }
        return xml.append("    </dependency>\n").toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
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
}
