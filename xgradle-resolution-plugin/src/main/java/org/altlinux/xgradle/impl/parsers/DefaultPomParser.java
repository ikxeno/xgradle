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
package org.altlinux.xgradle.impl.parsers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.maven.PomHierarchyLoader;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.model.PomCoordinateFactory;
import org.apache.maven.model.Model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates Maven POM parsing by delegating responsibilities to specialised parser components.
 * Implements {@link PomParser}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPomParser implements PomParser {

    private final PomHierarchyLoader hierarchyLoader;

    private final PomPropertiesCollector propertiesCollector = new PomPropertiesCollector();
    private final PomCoordinateFactory coordinateFactory = new PomCoordinateFactory();
    private final PomDependencyManagementParser dependencyManagementParser = new PomDependencyManagementParser();
    private final PomDependenciesParser dependenciesParser = new PomDependenciesParser();

    @Inject
    DefaultPomParser(PomHierarchyLoader hierarchyLoader) {
        this.hierarchyLoader = hierarchyLoader;
    }

    @Override
    public MavenCoordinate parsePom(Path pomPath) {
        List<Model> hierarchy = hierarchyLoader.loadHierarchy(pomPath);
        if (hierarchy == null || hierarchy.isEmpty()) {
            return null;
        }
        return coordinateFactory.create(hierarchy.get(hierarchy.size() - 1), pomPath);
    }

    @Override
    public Map<String, String> parseProperties(Path pomPath) {
        return Map.copyOf(propertiesCollector.collect(hierarchyLoader.loadHierarchy(pomPath)));
    }

    @Override
    public List<MavenCoordinate> parseDependencies(Path pomPath) {
        List<Model> hierarchy = hierarchyLoader.loadHierarchy(pomPath);
        Map<String, String> properties = propertiesCollector.collect(hierarchy);
        Map<String, MavenCoordinate> managed =
                dependencyManagementParser.parse(hierarchy, properties, propertiesCollector);
        return new ArrayList<>(
                dependenciesParser.parse(hierarchy, properties, managed, propertiesCollector).values());
    }
}
