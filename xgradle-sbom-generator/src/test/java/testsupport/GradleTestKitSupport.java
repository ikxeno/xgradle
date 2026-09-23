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
package testsupport;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Shared helpers for Gradle TestKit integration/e2e test setup.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class GradleTestKitSupport {

    private static final String TESTLIBS_DIR_NAME = "testlibs";
    private static final String METADATA_DIR_NAME = "maven-metadata";
    private static final String INIT_SCRIPT_NAME = "xgradle-resolution-plugin.gradle";
    private static final String INIT_SCRIPT_TEMPLATE_RESOURCE = "xgradle-resolution-plugin-test.gradle";

    private GradleTestKitSupport() {
    }

    public static ResolvedEnvironment resolveEnvironment() {
        File pluginJar = findPluginJar();
        if (pluginJar == null) {
            throw new IllegalStateException("Could not find plugin jar in expected locations");
        }

        File testLibDir = findExistingDirectory(
                "../testlibs",
                "testlibs",
                "../../testlibs"
        );
        if (testLibDir == null) {
            throw new IllegalStateException(
                    "Could not find testlibs directory. user.dir=" + System.getProperty("user.dir")
            );
        }

        return new ResolvedEnvironment(pluginJar, testLibDir);
    }

    public static File createGradleUserHome(File tempDir) {
        return createDirectory(new File(tempDir, "gradleUserHome"));
    }

    public static File createDirectory(File directory) {
        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IllegalStateException("Could not create directory: " + directory.getAbsolutePath());
        }
        return directory;
    }

    public static File preparePluginsDirectory(
            File gradleUserHome,
            File pluginJar
    ) throws IOException {
        File pluginsDir = createDirectory(new File(gradleUserHome, "lib/plugins"));
        File pluginTarget = new File(pluginsDir, pluginJar.getName());
        Files.copy(pluginJar.toPath(), pluginTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return pluginsDir;
    }

    public static File writeInitScript(
            File tempDir,
            File pluginsDir
    ) throws IOException {
        File initScript = new File(tempDir, INIT_SCRIPT_NAME);
        String template = loadResource(INIT_SCRIPT_TEMPLATE_RESOURCE);
        String initScriptContent = template.replace(
                "{{PLUGINS_DIR}}",
                pluginsDir.getAbsolutePath().replace("\\", "\\\\")
        );
        Files.writeString(initScript.toPath(), initScriptContent);
        return initScript;
    }

    public static String copyTestLibsToProject(
            File testLibDir,
            Path projectDir
    ) throws IOException {
        Path testLibPath = projectDir.resolve(TESTLIBS_DIR_NAME);
        copyDirectory(testLibDir.toPath(), testLibPath);
        String testLibAbsolutePath = testLibPath.toFile().getAbsolutePath();
        Path metadata = testLibPath.resolve(METADATA_DIR_NAME).resolve("testlibs.xml");
        Files.writeString(metadata, Files.readString(metadata).replace("@TESTLIBS@", testLibAbsolutePath));
        return testLibAbsolutePath;
    }

    public static BuildResult runOfflineBuild(
            File projectDir,
            File gradleUserHome,
            File initScript,
            String testLibAbsolutePath,
            String format,
            String... extraArgs
    ) throws IOException {
        List<String> args = new ArrayList<>();
        args.add("--gradle-user-home");
        args.add(gradleUserHome.getAbsolutePath());
        args.add("--init-script");
        args.add(initScript.getAbsolutePath());
        args.add("build");
        args.add("--offline");
        args.add("-Dmaven.metadata.dir=" + Path.of(testLibAbsolutePath, METADATA_DIR_NAME));
        args.add("-Dmaven.poms.dir=" + testLibAbsolutePath);
        args.add("-Djava.library.dir=" + testLibAbsolutePath);
        args.add("-Dgenerate.sbom=" + format);

        if (extraArgs != null && extraArgs.length > 0) {
            args.addAll(Arrays.asList(extraArgs));
        }

        return withoutInstalledInitScripts(GradleRunner.create(), gradleUserHome.getParentFile())
                .withProjectDir(projectDir)
                .withArguments(args.toArray(new String[0]))
                .forwardOutput()
                .build();
    }

    /**
     * Gradle applies every script in its installation's init.d, and on ALT that
     * directory holds the installed xgradle. The build under test gets an installation
     * that links everything else, so only the plugin being tested is applied.
     */
    private static GradleRunner withoutInstalledInitScripts(GradleRunner runner, File tempDir) throws IOException {
        String home = System.getProperty("xgradle.test.gradleHome");
        if (home == null || !Files.isDirectory(Path.of(home, "init.d"))) {
            return runner;
        }
        Path installation = Files.createDirectories(tempDir.toPath().resolve("gradleInstallation"));
        try (Stream<Path> entries = Files.list(Path.of(home))) {
            entries.filter(entry -> !entry.getFileName().toString().equals("init.d"))
                    .forEach(entry -> link(installation.resolve(entry.getFileName()), entry));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return runner.withGradleInstallation(installation.toFile());
    }

    private static void link(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void copyDirectory(
            Path sourceDir,
            Path targetDir
    ) throws IOException {
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        try (Stream<Path> files = Files.walk(sourceDir)) {
            files.forEach(source -> {
                try {
                    Path target = targetDir.resolve(sourceDir.relativize(source));
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    private static String loadResource(String resourceName) throws IOException {
        try (InputStream input = GradleTestKitSupport.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static File findPluginJar() {
        File direct = findExistingFile(
                "../xgradle-resolution-plugin/build/dist/xgradle-resolution-plugin.jar",
                "xgradle-resolution-plugin/build/dist/xgradle-resolution-plugin.jar",
                "../xgradle-resolution-plugin/build/libs/xgradle-resolution-plugin.jar",
                "xgradle-resolution-plugin/build/libs/xgradle-resolution-plugin.jar"
        );
        if (direct != null) {
            return direct;
        }

        File versioned = findVersionedJar("../xgradle-resolution-plugin/build/libs", "xgradle-resolution-plugin-");
        if (versioned != null) {
            return versioned;
        }
        return findVersionedJar("xgradle-resolution-plugin/build/libs", "xgradle-resolution-plugin-");
    }

    private static File findVersionedJar(
            String directoryPath,
            String prefix
    ) {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            return null;
        }

        File[] candidates = directory.listFiles((dir, name) ->
                name.startsWith(prefix)
                        && name.endsWith(".jar")
                        && !name.endsWith("-sources.jar")
                        && !name.endsWith("-javadoc.jar")
        );
        if (candidates == null || candidates.length == 0) {
            return null;
        }
        Arrays.sort(candidates, (left, right) -> Long.compare(right.lastModified(), left.lastModified()));
        return candidates[0];
    }

    private static File findExistingFile(String... candidates) {
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    private static File findExistingDirectory(String... candidates) {
        for (String candidate : candidates) {
            File directory = new File(candidate);
            if (directory.isDirectory()) {
                return directory;
            }
        }
        return null;
    }

    public static final class ResolvedEnvironment {

        private final File pluginJar;
        private final File testLibDir;

        private ResolvedEnvironment(
                File pluginJar,
                File testLibDir
        ) {
            this.pluginJar = pluginJar;
            this.testLibDir = testLibDir;
        }

        public File getPluginJar() {
            return pluginJar;
        }

        public File getTestLibDir() {
            return testLibDir;
        }
    }
}
