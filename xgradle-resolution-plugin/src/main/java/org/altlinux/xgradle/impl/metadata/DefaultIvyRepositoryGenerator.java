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
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.model.ArtifactKey;
import org.altlinux.xgradle.impl.model.IvyRepository;
import org.altlinux.xgradle.impl.model.XmvnArtifact;
import org.altlinux.xgradle.impl.model.XmvnDependency;
import org.altlinux.xgradle.interfaces.metadata.IvyRepositoryGenerator;
import org.altlinux.xgradle.interfaces.metadata.MetadataIndex;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Writes one ivy module per installed module revision: an {@code ivy.xml} with
 * the dependencies listed in the XMvn metadata, the same list XMvn builds its
 * effective POM from, and symlinks to the installed files.
 *
 * <p>The repository is kept in a {@link RepositoryCache} under a fingerprint of
 * its content.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultIvyRepositoryGenerator implements IvyRepositoryGenerator {

    /** Bump when the generated layout or descriptors change, to drop old caches. */
    private static final String FORMAT_VERSION = "2";

    private final MetadataIndex index;
    private final Logger logger;
    private final Map<Path, IvyRepository> generated = new HashMap<>();

    @Inject
    DefaultIvyRepositoryGenerator(MetadataIndex index, Logger logger) {
        this.index = index;
        this.logger = logger;
    }

    @Override
    public synchronized IvyRepository generate(Path cacheDirectory) {
        return generated.computeIfAbsent(cacheDirectory, this::generateUncached);
    }

    private IvyRepository generateUncached(Path cacheDirectory) {
        List<IvyModule> modules = modules();
        try {
            Path root = new RepositoryCache(cacheDirectory, logger).obtain(fingerprint(modules), repo -> {
                for (IvyModule module : modules) {
                    module.writeTo(repo);
                }
                logger.info("Generated system ivy repository with {} modules", modules.size());
            });
            return new IvyRepository(root);
        } catch (IOException e) {
            throw new GradleException("Cannot write the system ivy repository to " + cacheDirectory, e);
        }
    }

    /**
     * Groups index entries into ivy modules. Entries of one module differ only
     * in extension and classifier; entries reached through an alias make an
     * alias module.
     */
    private List<IvyModule> modules() {
        Map<String, ModuleEntries> grouped = new TreeMap<>();
        index.entries().forEach((key, artifact) -> index.revision(key).ifPresent(rev -> grouped
                .computeIfAbsent(key.module() + ":" + rev,
                        id -> new ModuleEntries(key.getGroupId(), key.getArtifactId(), rev))
                .add(key, artifact)));
        return grouped.values().stream()
                .map(entries -> new IvyModule(
                        entries.org + "/" + entries.name + "/" + entries.rev,
                        IvyDescriptor.render(entries.org, entries.name, entries.rev,
                                entries.publishedExtension(), dependencies(entries)),
                        entries.files))
                .collect(Collectors.toList());
    }

    /**
     * Dependencies of a module on installed module revisions. An alias module depends
     * on the aliased artifact, its classifier and extension included. A real module takes
     * the required dependencies from its metadata that resolve to an installed artifact.
     */
    private List<IvyDescriptor.Dependency> dependencies(ModuleEntries module) {
        Optional<XmvnArtifact> aliasOf = module.aliasOf();
        if (aliasOf.isPresent()) {
            XmvnArtifact target = aliasOf.get();
            XmvnDependency artifact = XmvnDependency.builder(target.getGroupId(), target.getArtifactId())
                    .extension(target.getExtension())
                    .classifier(target.getClassifier())
                    .build();
            return List.of(new IvyDescriptor.Dependency(
                    target.getGroupId(), target.getArtifactId(), module.rev, artifact));
        }
        return module.requiredDependencies()
                .flatMap(dep -> index.revision(dep.toKey())
                        .map(rev -> new IvyDescriptor.Dependency(dep.getGroupId(), dep.getArtifactId(), rev, dep))
                        .stream())
                .collect(Collectors.toList());
    }

    /**
     * Hash of every descriptor and link the repository would contain. A cached
     * repository is reused only if it would be written with the same files.
     */
    private static String fingerprint(List<IvyModule> modules) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            Stream.concat(
                    Stream.of(FORMAT_VERSION),
                    modules.stream().flatMap(IvyModule::contentLines))
                    .forEach(line -> digest.update((line + "\n").getBytes(StandardCharsets.UTF_8)));
            return toHex(digest.digest()).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        return IntStream.range(0, bytes.length)
                .mapToObj(i -> String.format("%02x", bytes[i]))
                .collect(Collectors.joining());
    }

    /** Index entries of one module revision, collected before the module is rendered. */
    private static final class ModuleEntries {

        private final String org;
        private final String name;
        private final String rev;
        private final Map<String, Path> files = new TreeMap<>();
        private XmvnArtifact main;
        private XmvnArtifact pom;
        private XmvnArtifact alias;

        private ModuleEntries(String org, String name, String rev) {
            this.org = org;
            this.name = name;
            this.rev = rev;
        }

        /**
         * Adds an index entry of this module. The jar is the main artifact; a file with
         * another extension becomes the main artifact only if the module has no jar.
         */
        private void add(ArtifactKey key, XmvnArtifact artifact) {
            boolean isAlias = !key.getGroupId().equals(artifact.getGroupId())
                    || !key.getArtifactId().equals(artifact.getArtifactId());
            if (isAlias) {
                boolean plainJar = ArtifactKey.DEFAULT_EXTENSION.equals(artifact.getExtension())
                        && artifact.getClassifier().isEmpty();
                alias = alias == null || plainJar ? artifact : alias;
                return;
            }
            if (ArtifactKey.POM_EXTENSION.equals(key.getExtension())) {
                pom = pom == null ? artifact : pom;
                return;
            }
            boolean jar = ArtifactKey.DEFAULT_EXTENSION.equals(key.getExtension());
            if (key.getClassifier().isEmpty() && (main == null || jar)) {
                main = artifact;
            }
            if (artifact.getPath() != null) {
                String classifier = key.getClassifier().isEmpty() ? "" : "-" + key.getClassifier();
                files.put(name + "-" + rev + classifier + "." + key.getExtension(), artifact.getPath());
            }
        }

        /** The aliased artifact, unless these coordinates also have real artifacts. */
        private Optional<XmvnArtifact> aliasOf() {
            return main != null || pom != null ? Optional.empty() : Optional.ofNullable(alias);
        }

        private Optional<String> publishedExtension() {
            return Optional.ofNullable(main)
                    .filter(artifact -> artifact.getPath() != null)
                    .map(XmvnArtifact::getExtension);
        }

        private Stream<XmvnDependency> requiredDependencies() {
            XmvnArtifact source = main != null ? main : pom;
            return source == null ? Stream.empty() : source.requiredDependencies();
        }
    }
}
