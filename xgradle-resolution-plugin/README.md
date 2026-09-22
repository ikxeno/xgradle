**xgradle-resolution-plugin** is the Gradle-side component of **xgradle** — an **offline-first toolkit** for Gradle builds.

It is implemented as a **Gradle plugin applied to the Gradle instance** (not to a single project), so it must be
loaded early via a Gradle **init script**. The module is distributed as:

- **`xgradle-resolution-plugin.jar`** — the plugin JAR
- **`xgradle-resolution-plugin.gradle`** — init script that adds the JAR to the init classpath and applies the plugin

---

## Where it fits in RPM packaging

In RPM (and similar distro packaging) workflows **xgradle-resolution-plugin** is typically used in the **`%build`** section.

It makes the *build itself* reproducible and offline-friendly by teaching Gradle to consume the **system/local artifact
set** (prepared by packaging) rather than downloading from the network.

---

## What it does

### 1) System dependency resolution (projects)
- Adds a **flatDir** repository for system JAR directories (scanned recursively).
- Uses Maven **POM metadata** from a system directory to drive resolution:
    - versions / BOM-managed versions
    - controlled transitive dependencies
    - substitutions / mapping to system artifacts

### 2) Local Gradle plugin resolution (Settings `pluginManagement`)
- Configures `pluginManagement.repositories` to include the same system JAR directories,
  allowing Gradle plugins to be resolved from local/system artifacts.

### 3) Optional SBOM generation
- If `generate.sbom` is set to `spdx` or `cyclonedx`, xgradle-resolution-plugin generates an SBOM report
  from resolved build artifacts.
- Report path:
  - `build/reports/xgradle/sbom-spdx.json`
  - `build/reports/xgradle/sbom-cyclonedx.json`

---

## How it works

Every ALT Java package installs XMvn metadata (`/usr/share/maven-metadata/*.xml`): the exact
coordinates of each installed jar and POM, its path, aliases (`%mvn_alias`), compat versions
(`%mvn_compat_version`) and its runtime dependencies. The plugin reads the metadata repositories
and `ignoreDuplicateMetadata` from the XMvn configuration (the project's `.xmvn/`, then the XDG
user and system directories), resolves artifacts with the same rules as XMvn and generates an ivy repository
from it in the Gradle user home (`caches/xgradle/ivy`): one `ivy.xml` per installed module,
built from the metadata dependency list, plus symlinks to the installed files. Packages installed
without XMvn metadata, as xgradle-cli installs Gradle-built packages, are read from their POMs
(`/usr/share/maven-poms/X/Y.pom` with the jar `/usr/share/java/X/Y.jar`); XMvn metadata wins
whenever both describe a module. The repository
is added first to every project and to plugin management, so Gradle resolves system artifacts
and their transitive dependencies itself, exactly as Maven does under XMvn.

## Configuration

xgradle-resolution-plugin is configured via **system properties** or the user config file
`~/.xgradle/xgradle.config` (Java properties format). System properties set with
`-D` take precedence.

| Property | Meaning |
|---|---|
| `maven.metadata.dir` | One or more directories or files with **XMvn metadata** (comma-separated). Overrides the metadata repositories of the XMvn configuration (on ALT `/usr/share/maven-metadata` and `/usr/share/javapackages-bootstrap/maven-metadata`). |
| `maven.poms.dir` | Root of POMs installed **without** XMvn metadata, such as packages installed by xgradle-cli (default `/usr/share/maven-poms`). |
| `java.library.dir` | Root of the jars matching those POMs: `maven.poms.dir/X/Y.pom` pairs with `java.library.dir/X/Y.jar` (default `/usr/share/java`). |
| `disable.xgradle=true` | Completely disables xgradle plugin logic for the current build. |
| `disable.logo=true` | Disable ASCII banner printing. |
| `enable.ansi.color=true` | Enable ANSI colors in xgradle logs. |
| `generate.sbom` | SBOM format: `spdx` or `cyclonedx`. |

Example config file (`~/.xgradle/xgradle.config`):

```
maven.metadata.dir=/usr/share/maven-metadata
disable.xgradle=false
disable.logo=true
enable.ansi.color=true
generate.sbom=spdx
```

## Usage Example:

```bash
gradle build \
  -Dmaven.metadata.dir=/usr/share/maven-metadata \
  -Dgenerate.sbom=cyclonedx \
  --offline
```

```
[root@82c69e916850 plumelib-options]# gradle build -Dmaven.poms.dir=/usr/share/maven-poms -Djava.library.dir=/usr/share/java -Dgenerate.sbom=cyclonedx --offline

Welcome to Gradle 8.14.3!

Here are the highlights of this release:
 - Java 24 support
 - GraalVM Native Image toolchain selection
 - Enhancements to test reporting
 - Build Authoring improvements

For more details see https://docs.gradle.org/8.14.3/release-notes.html

Starting a Gradle Daemon, 1 stopped Daemon could not be reused, use --status for details
                                              _ _
                     __  ____ _ _ __ __ _  __| | | ___
                     \ \/ / _` | '__/ _` |/ _` | |/ _ \
                      >  < (_| | | | (_| | (_| | |  __/
                     /_/\_\__, |_|  \__,_|\__,_|_|\___| v 0.2.1
                          |___/


POM index built: 48 artifacts, 21 groups

> Configure project :
Skipping core plugin: java-library
Skipping core plugin: maven-publish
POM index built: 48 artifacts, 21 groups
>>> Processing transitive dependencies
Dropped 1 artifacts after rescanning: [io.github.toolfactory:narcissus]

--- 
===== APPLYING SYSTEM DEPENDENCY VERSIONS ===== ---

--- Initial dependencies ---
Found 7 dependencies

--- Resolved system artifacts ---
Resolved 20 artifacts

--- Test context dependencies ---
Test context dependencies: 21

--- ===== DEPENDENCY RESOLUTION COMPLETED ===== ---

--- Added artifacts to configurations ---
Configuration 'testRuntimeOnly' (6 artifacts):
 - org.junit.jupiter:junit-jupiter-engine:5.10.2
 - org.junit.jupiter:junit-jupiter-api:5.10.2
 - org.opentest4j:opentest4j:1.3.0
 - org.junit.platform:junit-platform-commons:1.10.2
 - org.apiguardian:apiguardian-api:1.1.2
 - org.junit.platform:junit-platform-engine:1.10.2
Configuration 'compileOnly' (18 artifacts):
 - io.github.classgraph:classgraph:4.8.184
 - org.junit.jupiter:junit-jupiter-api:5.10.2
 - org.plumelib:reflection-util:unspecified
 - org.plumelib:hashmap-util:unspecified
 - org.checkerframework:checker-qual:3.52.0
 - org.apache.commons:commons-lang3:3.19.0
 - com.google.code.findbugs:jsr305:3.0.2
 - org.opentest4j:opentest4j:1.3.0
 - org.ow2.asm:asm:9.9
 - com.google.guava:failureaccess:1.0.1
 - org.plumelib:plume-util:1.12.2
 - org.junit.jupiter:junit-jupiter-params:5.10.2
 - org.plumelib:options:2.0.3
 - org.junit.platform:junit-platform-commons:1.10.2
 - com.google.guava:guava:31.0.1-jre
 - org.apiguardian:apiguardian-api:1.1.2
 - org.apache.commons:commons-text:1.15.0
 - com.univocity:univocity-parsers:2.9.1
Configuration 'testCompileOnly' (18 artifacts):
 - io.github.classgraph:classgraph:4.8.184
 - org.junit.jupiter:junit-jupiter-api:5.10.2
 - org.plumelib:reflection-util:unspecified
 - org.plumelib:hashmap-util:unspecified
 - org.checkerframework:checker-qual:3.52.0
 - org.apache.commons:commons-lang3:3.19.0
 - com.google.code.findbugs:jsr305:3.0.2
 - org.opentest4j:opentest4j:1.3.0
 - org.ow2.asm:asm:9.9
 - com.google.guava:failureaccess:1.0.1
 - org.plumelib:plume-util:1.12.2
 - org.junit.jupiter:junit-jupiter-params:5.10.2
 - org.plumelib:options:2.0.3
 - org.junit.platform:junit-platform-commons:1.10.2
 - com.google.guava:guava:31.0.1-jre
 - org.apiguardian:apiguardian-api:1.1.2
 - org.apache.commons:commons-text:1.15.0
 - com.univocity:univocity-parsers:2.9.1
Configuration 'implementation' (4 artifacts):
 - io.github.classgraph:classgraph:4.8.184
 - org.plumelib:reflection-util:unspecified
 - org.apache.commons:commons-lang3:3.19.0
 - org.apache.commons:commons-text:1.15.0
Configuration 'testImplementation' (4 artifacts):
 - org.junit.jupiter:junit-jupiter-api:5.10.2
 - org.opentest4j:opentest4j:1.3.0
 - org.junit.platform:junit-platform-commons:1.10.2
 - org.apiguardian:apiguardian-api:1.1.2

> Task :compileJava
warning: [options] source value 8 is obsolete and will be removed in a future release
warning: [options] target value 8 is obsolete and will be removed in a future release
warning: [options] To suppress warnings about obsolete options, use -Xlint:-options.
/app/plumelib-options/src/main/java/org/plumelib/options/OptionsDoclet.java:1208: warning: [deprecation] removeStart(String,String) in StringUtils has been deprecated
      startDelim = StringUtils.removeStart("* ", startDelim);
                              ^
/app/plumelib-options/src/main/java/org/plumelib/options/OptionsDoclet.java:1209: warning: [deprecation] removeStart(String,String) in StringUtils has been deprecated
      endDelim = StringUtils.removeStart("* ", endDelim);
                            ^
5 warnings

> Task :compileTestJava
warning: [options] source value 8 is obsolete and will be removed in a future release
warning: [options] target value 8 is obsolete and will be removed in a future release
warning: [options] To suppress warnings about obsolete options, use -Xlint:-options.
3 warnings
Generated cyclonedx SBOM: /app/plumelib-options/build/reports/xgradle/sbom-cyclonedx.json

[Incubating] Problems report is available at: file:///app/plumelib-options/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.3/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 4s
5 actionable tasks: 5 executed
```
