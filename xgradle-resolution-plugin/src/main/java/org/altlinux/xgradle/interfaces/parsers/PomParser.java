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
package org.altlinux.xgradle.interfaces.parsers;

import org.altlinux.xgradle.impl.model.MavenCoordinate;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Parses Maven POM files and extracts coordinates and dependencies.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface PomParser {

    /**
     * Parses artifact coordinates from a POM file.
     *
     * @param pomPath path to the POM file
     *
     * @return parsed coordinates or null on failure
     */
    MavenCoordinate parsePom(Path pomPath);

    /**
     * Parses dependencies from the dependencies section.
     *
     * @param pomPath path to the POM file
     *
     * @return list of dependency coordinates (may be empty)
     */
    List<MavenCoordinate> parseDependencies(Path pomPath);

    /**
     * Parses properties section of a POM file.
     *
     * @param pomPath path to the POM file
     *
     * @return map of property names to values
     */
    Map<String, String> parseProperties(Path pomPath);

}
