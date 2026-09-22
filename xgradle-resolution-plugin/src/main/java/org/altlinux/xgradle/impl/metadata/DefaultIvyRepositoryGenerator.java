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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
 * <p>The repository is written to a directory named after a fingerprint of the
 * metadata files, first into a temporary directory that is then renamed, so
 * concurrent builds never see a half-written repository.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultIvyRepositoryGenerator implements IvyRepositoryGenerator {

    /** Bump when the generated layout or descriptors change, to drop old caches. */
    private static final String FORMAT_VERSION = "1";
    private static final String COMPLETE_MARKER = ".complete";
    private static final String MISSING_FILE = "missing-dependencies.txt";
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
        Path root = cacheDirectory.resolve(fingerprint());
        try {
            if (!Files.isRegularFile(root.resolve(COMPLETE_MARKER))) {
                write(cacheDirectory, root);
            }
            return new IvyRepository(root, Files.readAllLines(root.resolve(MISSING_FILE)));
        } catch (IOException | UncheckedIOException e) {
            throw new GradleException("Cannot write the system ivy repository to " + root, e);
        }
    }

    private void write(Path cacheDirectory, Path root) throws IOException {
        Files.createDirectories(cacheDirectory);
        Path tmp = Files.createTempDirectory(cacheDirectory, root.getFileName() + ".");

        Map<String, Module> modules = modules();
        modules.values().forEach(module -> writeModule(tmp, module));
        List<String> missing = modules.values().stream()
                .flatMap(module -> missingDependencies(module).stream())
                .sorted()
                .collect(Collectors.toList());
        Files.write(tmp.resolve(MISSING_FILE), missing);
        Files.createFile(tmp.resolve(COMPLETE_MARKER));

        try {
            Files.move(tmp, root, StandardCopyOption.ATOMIC_MOVE);
            logger.info("Generated system ivy repository with {} modules in {}", modules.size(), root);
        } catch (FileAlreadyExistsException | DirectoryNotEmptyException e) {
            logger.info("System ivy repository {} was generated concurrently", root);
            deleteRecursively(tmp);
        }
    }

    /**
     * Groups index entries into ivy modules. Entries of one module differ only
     * in extension and classifier; entries reached through an alias make an
     * alias module.
     */
    private Map<String, Module> modules() {
        Map<String, Module> modules = new TreeMap<>();
        index.entries().forEach((key, artifact) -> moduleRevision(key).ifPresent(rev -> {
            Module module = modules.computeIfAbsent(
                    key.getGroupId() + ":" + key.getArtifactId() + ":" + rev,
                    id -> new Module(key.getGroupId(), key.getArtifactId(), rev));
            module.add(key, artifact);
        }));
        modules.values().forEach(Module::resolveAlias);
        return modules;
    }

    /**
     * Revision of the module an index key belongs to, as XMvn resolves it:
     * the compat version for a compat artifact, the upstream version otherwise.
     */
    private Optional<String> moduleRevision(ArtifactKey key) {
        XmvnArtifact exact = index.entries().get(key);
        if (exact != null && !key.isSystemVersion()) {
            return Optional.of(key.getVersion());
        }
        return index.resolve(key.withVersion(ArtifactKey.SYSTEM_VERSION)).map(XmvnArtifact::getVersion);
    }

    private void writeModule(Path repo, Module module) {
        Path dir = repo.resolve(module.org).resolve(module.name).resolve(module.rev);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("ivy.xml"), descriptor(module), StandardCharsets.UTF_8);
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
                .flatMap(dep -> moduleRevision(dep.toKey())
                        .map(rev -> new ResolvedDependency(dep.getGroupId(), dep.getArtifactId(), rev, dep))
                        .stream());
    }

    private List<String> missingDependencies(Module module) {
        if (module.aliasOf != null) {
            return List.of();
        }
        return module.declaredDependencies()
                .filter(dep -> moduleRevision(dep.toKey()).isEmpty())
                .map(dep -> module.org + ":" + module.name + ":" + module.rev + " -> " + dep)
                .collect(Collectors.toList());
    }

    private static String dependency(ResolvedDependency resolved) {
        StringBuilder xml = new StringBuilder("    <dependency org=\"").append(escape(resolved.org))
                .append("\" name=\"").append(escape(resolved.name))
                .append("\" rev=\"").append(escape(resolved.rev))
                .append("\" conf=\"").append(CONF).append("->").append(CONF).append("\"");

        XmvnDependency dep = resolved.dependency;
        boolean customArtifact = dep != null && !"pom".equals(dep.getExtension())
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

    private String fingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(FORMAT_VERSION.getBytes(StandardCharsets.UTF_8));
            index.artifacts().stream()
                    .map(XmvnArtifact::getMetadataFile)
                    .distinct()
                    .map(DefaultIvyRepositoryGenerator::fileStamp)
                    .forEach(stamp -> digest.update(stamp.getBytes(StandardCharsets.UTF_8)));
            return toHex(digest.digest()).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String fileStamp(Path file) {
        try {
            return file + "\0" + Files.size(file) + "\0" + Files.getLastModifiedTime(file).toMillis() + "\n";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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

    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.delete(path);
            }
        }
    }

    private static final class Module {

        private final String org;
        private final String name;
        private final String rev;
        private final Map<String, Path> files = new TreeMap<>();
        private XmvnArtifact main;
        private XmvnArtifact pom;
        private XmvnArtifact aliasOf;

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
            if ("pom".equals(key.getExtension())) {
                pom = pom == null ? artifact : pom;
                return;
            }
            if (key.getClassifier().isEmpty()) {
                main = artifact;
            }
            if (artifact.getPath() != null) {
                String classifier = key.getClassifier().isEmpty() ? "" : "-" + key.getClassifier();
                files.put(name + "-" + rev + classifier + "." + key.getExtension(), artifact.getPath());
            }
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
