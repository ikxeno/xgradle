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
package org.altlinux.xgradle.impl.resolution;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.impl.model.XmvnDependency;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;
import org.altlinux.xgradle.interfaces.resolution.Order;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Finds transitive dependencies of the resolved system artifacts that no
 * installed package provides. The generated ivy descriptors leave such
 * dependencies out, so without this step the build would silently run with
 * a smaller classpath than the metadata asks for.
 *
 * <p>Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
@Order(750)
final class FindMissingTransitivesStep implements ResolutionStep {

    private final MetadataIndex index;

    @Inject
    FindMissingTransitivesStep(MetadataIndex index) {
        this.index = index;
    }

    @Override
    public String name() {
        return "find-missing-transitives";
    }

    @Override
    public void execute(ResolutionContext ctx) {
        Deque<XmvnArtifact> queue = new ArrayDeque<>();
        Set<XmvnArtifact> visited = new HashSet<>();
        ctx.getSystemArtifacts().values().stream()
                .map(coordinate -> index.resolveModule(
                        coordinate.getGroupId(),
                        coordinate.getArtifactId(),
                        ctx.getRequestedVersions().getOrDefault(
                                coordinate.getGroupId() + ":" + coordinate.getArtifactId(), Set.of())))
                .flatMap(Optional::stream)
                .forEach(queue::add);

        ctx.getSkipped().clear();
        while (!queue.isEmpty()) {
            XmvnArtifact artifact = queue.poll();
            if (!visited.add(artifact)) {
                continue;
            }
            artifact.getDependencies().stream()
                    .filter(dependency -> !dependency.isOptional())
                    .forEach(dependency -> index.resolve(dependency.toKey()).ifPresentOrElse(
                            queue::add,
                            () -> ctx.markSkipped(describe(artifact, dependency))));
        }
    }

    private static String describe(XmvnArtifact artifact, XmvnDependency dependency) {
        return dependency + " (required by " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ")";
    }
}
