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

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.altlinux.xgradle.impl.metadata.Xml.child;
import static org.altlinux.xgradle.impl.metadata.Xml.children;
import static org.altlinux.xgradle.impl.metadata.Xml.text;

/**
 * Resolver settings read from the XMvn configuration files, in the order and
 * with the merge rules of XMvn's {@code DefaultConfigurator}: the project's
 * {@code .xmvn/} first, then the XDG user and system directories, where ALT
 * ships {@code /usr/share/xmvn/configuration.xml}.
 *
 * <p>Only the settings the XMvn resolver uses are read: the metadata
 * repositories, which accumulate across files, and
 * {@code ignoreDuplicateMetadata}, where the first file that sets it wins
 * and XMvn's built-in default is {@code true}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class XmvnConfiguration {

    private static final boolean DEFAULT_IGNORE_DUPLICATES = true;

    private final List<Path> metadataRepositories;
    private final boolean ignoreDuplicateMetadata;

    XmvnConfiguration(List<Path> metadataRepositories, boolean ignoreDuplicateMetadata) {
        this.metadataRepositories = List.copyOf(metadataRepositories);
        this.ignoreDuplicateMetadata = ignoreDuplicateMetadata;
    }

    /**
     * Loads the configuration visible to XMvn started in {@code currentDir}.
     */
    public static XmvnConfiguration load(Path currentDir) {
        return load(currentDir, System.getenv(), Path.of(System.getProperty("user.home")),
                System.getProperty("xmvn.config.sandbox") != null);
    }

    /**
     * @param userHome the home directory when {@code $HOME} is unset or empty
     * @param sandbox  XMvn's {@code xmvn.config.sandbox}: read only the project's {@code .xmvn/}
     */
    public static XmvnConfiguration load(Path currentDir, Map<String, String> env, Path userHome, boolean sandbox) {
        List<Element> settings = configFiles(currentDir, env, userHome, sandbox)
                .map(XmvnConfiguration::resolverSettings)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        List<Path> repositories = settings.stream()
                .flatMap(resolver -> children(child(resolver, "metadataRepositories"), "repository"))
                .map(repository -> currentDir.resolve(text(repository)))
                .distinct()
                .collect(Collectors.toList());
        boolean ignoreDuplicates = settings.stream()
                .map(resolver -> child(resolver, "ignoreDuplicateMetadata"))
                .filter(Objects::nonNull)
                .map(element -> Boolean.parseBoolean(text(element)))
                .findFirst()
                .orElse(DEFAULT_IGNORE_DUPLICATES);

        return new XmvnConfiguration(repositories, ignoreDuplicates);
    }

    /**
     * Metadata repositories in XMvn order: settings from more specific files first.
     * A relative path is taken from the build directory, as XMvn takes it from the
     * directory Maven runs in.
     */
    public List<Path> getMetadataRepositories() {
        return metadataRepositories;
    }

    public boolean isIgnoreDuplicateMetadata() {
        return ignoreDuplicateMetadata;
    }

    /**
     * Configuration files from the most to the least specific, as XMvn lists them.
     */
    private static Stream<Path> configFiles(Path currentDir, Map<String, String> env, Path userHome, boolean sandbox) {
        Path reactor = currentDir.resolve(".xmvn");
        Stream<Path> reactorFiles = Stream.concat(
                sortedFiles(reactor.resolve("config.d")),
                Stream.of(reactor.resolve("configuration.xml")));
        if (sandbox) {
            return reactorFiles.filter(Files::isRegularFile);
        }

        Path home = Path.of(envOrDefault(env, "HOME", userHome.toString()));
        Stream<String> xdgBases = Stream.of(
                        Stream.of(envOrDefault(env, "XDG_CONFIG_HOME", home.resolve(".config").toString())),
                        Stream.of(envOrDefault(env, "XDG_DATA_HOME", home.resolve(".local/share").toString())),
                        Arrays.stream(envOrDefault(env, "XDG_CONFIG_DIRS", "/etc/xdg").split(":+")),
                        Arrays.stream(envOrDefault(env, "XDG_DATA_DIRS", "/usr/local/share:/usr/share").split(":+")))
                .flatMap(bases -> bases);
        Stream<Path> xdgFiles = xdgBases
                .filter(base -> !base.isEmpty())
                .map(Path::of)
                .filter(Path::isAbsolute)
                .map(base -> base.resolve("xmvn"))
                .flatMap(base -> Stream.concat(
                        sortedFiles(base.resolve("config.d")),
                        Stream.of(base.resolve("configuration.xml"))));

        return Stream.concat(reactorFiles, xdgFiles).filter(Files::isRegularFile);
    }

    /** Like XMvn, an empty variable counts as unset. */
    private static String envOrDefault(Map<String, String> env, String name, String defaultValue) {
        String value = env.get(name);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    private static Stream<Path> sortedFiles(Path directory) {
        if (!Files.isDirectory(directory)) {
            return Stream.empty();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.sorted().collect(Collectors.toList()).stream();
        } catch (IOException e) {
            throw new GradleException("Cannot list XMvn configuration directory " + directory, e);
        }
    }

    private static Optional<Element> resolverSettings(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            Element root = Xml.parse(in).getDocumentElement();
            return Optional.ofNullable(child(root, "resolverSettings"));
        } catch (IOException | SAXException e) {
            throw new GradleException("Cannot read XMvn configuration " + file + ": " + e.getMessage(), e);
        }
    }
}
