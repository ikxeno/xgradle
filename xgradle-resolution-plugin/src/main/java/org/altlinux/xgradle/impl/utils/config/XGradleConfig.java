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
package org.altlinux.xgradle.impl.utils.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

/**
 * Loads xgradle configuration from ~/.xgradle/xgradle.config
 * and exposes property resolution with system property precedence.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class XGradleConfig {

    private static final String CONFIG_DIR = ".xgradle";
    private static final String CONFIG_FILE = "xgradle.config";

    private static final Object LOCK = new Object();
    private static final Properties PROPERTIES = new Properties();
    private static volatile boolean loaded = false;

    private static final Set<String> SUPPORTED_KEYS = Set.of(
            "java.library.dir",
            "maven.poms.dir",
            "maven.metadata.dir",
            "disable.xgradle",
            "disable.logo",
            "enable.ansi.color",
            "xgradle.scan.depth",
            "generate.sbom"
    );

    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    public static String getConfigProperty(String key) {
        return getConfigValue(key);
    }

    public static String getProperty(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null) {
            return systemValue;
        }
        String configValue = getConfigValue(key);
        return configValue != null ? configValue : defaultValue;
    }

    public static int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static void initSystemProperties() {
        ensureLoaded();
        SUPPORTED_KEYS.stream()
                .filter(key -> System.getProperty(key) == null)
                .forEach(key -> {
                    String value = getConfigValue(key);
                    if (value != null) {
                        System.setProperty(key, value);
                    }
                });
    }

    static void resetForTests() {
        synchronized (LOCK) {
            PROPERTIES.clear();
            loaded = false;
        }
    }

    private static String getConfigValue(String key) {
        ensureLoaded();
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (LOCK) {
            if (loaded) {
                return;
            }
            loadFromDisk();
            loaded = true;
        }
    }

    private static void loadFromDisk() {
        Path configPath = resolveConfigPath();
        if (configPath == null || !Files.isRegularFile(configPath)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            PROPERTIES.load(reader);
        } catch (IOException ignored) {
        }
    }

    private static Path resolveConfigPath() {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return null;
        }
        return Path.of(home, CONFIG_DIR, CONFIG_FILE);
    }

    private XGradleConfig() {
    }
}
