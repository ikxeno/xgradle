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
package org.altlinux.xgradle.impl.resolvers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.resolvers.ArtifactResolver;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.gradle.api.logging.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
/**
 * Resolver for Artifact.
 * Implements {@link ArtifactResolver}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

@Singleton
public final class DefaultArtifactResolver implements ArtifactResolver {

    private final PomFinder pomFinder;

    private Map<String, MavenCoordinate> systemArtifacts = Collections.emptyMap();
    private Set<String> notFound = Collections.emptySet();

    @Inject
    public DefaultArtifactResolver(PomFinder pomFinder) {
        this.pomFinder = pomFinder;
    }

    /**
     * Looks the declared {@code groupId:artifactId} keys up among the installed
     * artifacts. Transitive dependencies are left to Gradle, which reads them from
     * the ivy descriptors generated from the same metadata.
     */
    @Override
    public void resolve(Set<String> dependencies, Logger logger) {
        Map<String, MavenCoordinate> found = new LinkedHashMap<>();
        Set<String> missing = new LinkedHashSet<>();
        dependencies.stream().sorted().forEach(key -> {
            String[] ga = key.split(":", 3);
            MavenCoordinate coordinate = ga.length < 2 ? null : pomFinder.findPomForArtifact(ga[0], ga[1]);
            if (coordinate == null) {
                missing.add(key);
            } else {
                found.put(key, coordinate);
            }
        });
        systemArtifacts = found;
        notFound = missing;
    }

    @Override
    public void filter() {
        systemArtifacts.entrySet().removeIf(e -> e.getValue().isPomOnly());
    }

    @Override
    public Map<String, MavenCoordinate> getSystemArtifacts() {
        return systemArtifacts;
    }

    @Override
    public Set<String> getNotFoundDependencies() {
        return notFound;
    }
}
