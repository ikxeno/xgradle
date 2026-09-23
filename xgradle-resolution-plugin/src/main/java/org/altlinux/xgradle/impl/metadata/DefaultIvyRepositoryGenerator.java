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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Writes one ivy module per installed module revision: an {@code ivy.xml}
 * built from the XMvn dependency list, the way XMvn builds an effective POM
 * for Maven, plus symlinks to the installed files.
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
    private static final String CONF = "default";

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
        Collection<Module> modules = modules().values();
        try {
            Path root = new RepositoryCache(cacheDirectory, logger).obtain(fingerprint(modules), repo -> {
                modules.forEach(module -> writeModule(repo, module));
                logger.info("Generated system ivy repository with {} modules", modules.size());
            });
            return new IvyRepository(root);
        } catch (IOException | UncheckedIOException e) {
            throw new GradleException("Cannot write the system ivy repository to " + cacheDirectory, e);
        }
    }

    /**
     * Groups index entries into ivy modules. Entries of one module differ only
     * in extension and classifier; entries reached through an alias make an
     * alias module.
     */
    private Map<String, Module> modules() {
        Map<String, Module> modules = new TreeMap<>();
        index.entries().forEach((key, artifact) -> index.revision(key).ifPresent(rev -> {
            Module module = modules.computeIfAbsent(
                    key.getGroupId() + ":" + key.getArtifactId() + ":" + rev,
                    id -> new Module(key.getGroupId(), key.getArtifactId(), rev));
            module.add(key, artifact);
        }));
        modules.values().forEach(Module::resolveAlias);
        modules.values().forEach(module -> module.descriptor = descriptor(module));
        return modules;
    }

    private void writeModule(Path repo, Module module) {
        Path dir = repo.resolve(module.directory());
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("ivy.xml"), module.descriptor, StandardCharsets.UTF_8);
            for (Map.Entry<String, Path> file : module.files.entrySet()) {
                Files.createSymbolicLink(dir.resolve(file.getKey()), file.getValue());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String descriptor(Module module) {
        StringBuilder xml = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<ivy-module version=\"2.0\" xmlns:m=\"http://ant.apache.org/ivy/maven\">\n")
                .append("  <info organisation=\"").append(escape(module.org))
                .append("\" module=\"").append(escape(module.name))
                .append("\" revision=\"").append(escape(module.rev))
                .append("\" status=\"release\"/>\n")
                .append("  <configurations>\n    <conf name=\"").append(CONF).append("\"/>\n  </configurations>\n");

        // An empty <publications/> is required: without it ivy assumes a jar named after the module.
        Optional<XmvnArtifact> published = module.publishedArtifact();
        if (published.isPresent()) {
            String ext = escape(published.get().getExtension());
            xml.append("  <publications>\n    <artifact name=\"").append(escape(module.name))
                    .append("\" type=\"").append(ext).append("\" ext=\"").append(ext)
                    .append("\" conf=\"").append(CONF).append("\"/>\n  </publications>\n");
        } else {
            xml.append("  <publications/>\n");
        }

        xml.append("  <dependencies>\n");
        dependencies(module).forEach(dep -> xml.append(dependency(dep)));
        return xml.append("  </dependencies>\n</ivy-module>\n").toString();
    }

    /**
     * Dependencies of a module as {@code (dependency, revision)} pairs. An alias module
     * depends on the aliased module; a real module takes the non-optional dependencies
     * from its metadata that resolve to an installed artifact.
     */
    private Stream<ResolvedDependency> dependencies(Module module) {
        if (module.aliasOf != null) {
            return Stream.of(new ResolvedDependency(
                    module.aliasOf.getGroupId(), module.aliasOf.getArtifactId(), module.rev, null));
        }
        return module.declaredDependencies()
                .flatMap(dep -> index.revision(dep.toKey())
                        .map(rev -> new ResolvedDependency(dep.getGroupId(), dep.getArtifactId(), rev, dep))
                        .stream());
    }

    private static String dependency(ResolvedDependency resolved) {
        StringBuilder xml = new StringBuilder("    <dependency org=\"").append(escape(resolved.org))
                .append("\" name=\"").append(escape(resolved.name))
                .append("\" rev=\"").append(escape(resolved.rev))
                .append("\" conf=\"").append(CONF).append("->").append(CONF).append("\"");

        XmvnDependency dep = resolved.dependency;
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

    /**
     * Hash of everything the repository consists of, so a repository is reused
     * exactly when it would be written the same way, whatever changed in the
     * metadata, the installed files or the configuration.
     */
    private static String fingerprint(Collection<Module> modules) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            Stream.concat(
                    Stream.of(FORMAT_VERSION),
                    modules.stream().flatMap(Module::contentLines))
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

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final class Module {

        private final String org;
        private final String name;
        private final String rev;
        private final Map<String, Path> files = new TreeMap<>();
        private XmvnArtifact main;
        private XmvnArtifact pom;
        private XmvnArtifact aliasOf;
        private String descriptor;

        private Module(String org, String name, String rev) {
            this.org = org;
            this.name = name;
            this.rev = rev;
        }

        private void add(ArtifactKey key, XmvnArtifact artifact) {
            boolean alias = !key.getGroupId().equals(artifact.getGroupId())
                    || !key.getArtifactId().equals(artifact.getArtifactId());
            if (alias) {
                aliasOf = aliasOf == null ? artifact : aliasOf;
                return;
            }
            if (ArtifactKey.POM_EXTENSION.equals(key.getExtension())) {
                pom = pom == null ? artifact : pom;
                return;
            }
            // The jar is the main artifact; another extension only when the module has no jar.
            boolean jar = ArtifactKey.DEFAULT_EXTENSION.equals(key.getExtension());
            if (key.getClassifier().isEmpty() && (main == null || jar)) {
                main = artifact;
            }
            if (artifact.getPath() != null) {
                String classifier = key.getClassifier().isEmpty() ? "" : "-" + key.getClassifier();
                files.put(name + "-" + rev + classifier + "." + key.getExtension(), artifact.getPath());
            }
        }

        private String directory() {
            return org + "/" + name + "/" + rev;
        }

        private Stream<String> contentLines() {
            return Stream.concat(
                    Stream.of(directory(), descriptor),
                    files.entrySet().stream().map(file -> file.getKey() + " -> " + file.getValue()));
        }

        /** Coordinates that also have real artifacts are not treated as an alias. */
        private void resolveAlias() {
            if (main != null || pom != null) {
                aliasOf = null;
            }
        }

        private Optional<XmvnArtifact> publishedArtifact() {
            return Optional.ofNullable(main).filter(artifact -> artifact.getPath() != null);
        }

        private Stream<XmvnDependency> declaredDependencies() {
            XmvnArtifact source = main != null ? main : pom;
            return source == null
                    ? Stream.empty()
                    : source.getDependencies().stream().filter(dep -> !dep.isOptional());
        }
    }

    private static final class ResolvedDependency {

        private final String org;
        private final String name;
        private final String rev;
        private final XmvnDependency dependency;

        private ResolvedDependency(String org, String name, String rev, XmvnDependency dependency) {
            this.org = org;
            this.name = name;
            this.rev = rev;
            this.dependency = dependency;
        }
    }
}
