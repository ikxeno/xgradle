<div align="center">
  <img alt="xgradle logo" src="gradle/xgradle-logo.png" width="500">
</div>

### 🤔 What`s xgradle?

**xgradle** is an **offline-first toolkit** for Gradle builds.

Instead of relying on remote Maven repositories, xgradle lets you build Java/Gradle projects
using **artifacts already present on the system** (JARs + POM metadata). It is designed to work
in synergy with **XMvn** — Fedora’s tooling for **Apache Maven** that manages and uses a
**system-wide artifact repository** to resolve Maven artifacts in **offline mode** and support
distribution packaging workflows. xgradle brings the same “system artifacts first” approach to
**Gradle**, complementing the Java ecosystem in Linux distributions and their package repositories.
The project is split into standalone components that work together:

XMvn: https://fedora-java.github.io/xmvn

- **[xgradle-resolution-plugin](xgradle-resolution-plugin/README.md) (Gradle plugin / init script)** — configures Gradle to resolve:
    - Project dependencies from the installed artifacts described by XMvn metadata
    - Gradle plugins via pluginManagement repositories
    - Build script classpath declared in `buildscript { }` blocks

- **[xgradle-cli](xgradle-cli/README.md) (standalone CLI)** — prepares and maintains the local artifact set used by Gradle:
    - Registers local artifacts / BOMs for consistent versioning
    - Installs/exports JARs, POMs and related metadata into target directories
    - Optionally patches/redacts POM files to fit offline/system packaging needs

- **[xgradle-sbom-generator](xgradle-sbom-generator/README.md) (extension module)** — generates SBOM reports from resolved artifacts in:
    - SPDX JSON
    - CycloneDX JSON

The result: **reproducible builds in fully offline environments** (CI, air-gapped hosts, distro build farms).

---
### 📦 External Dependencies

**Gradle build plugin**
- com.gradleup.shadow:shadow-gradle-plugin

**Main (runtime / implementation)**
- org.apache.maven:maven-model
- org.apache.maven:maven-model-builder
- org.codehaus.plexus:plexus-utils
- org.jcommander:jcommander
- com.google.code.gson:gson
- org.slf4j:slf4j-api
- org.slf4j:slf4j-simple
- org.slf4j:log4j-over-slf4j
- com.google.inject:guice
- com.google.guava:guava
- com.google.guava:failureaccess
- jakarta.inject:jakarta.inject-api
- aopalliance:aopalliance
- org.ow2.asm:asm

**Tests**
- org.junit:junit-bom
- org.junit.jupiter:junit-jupiter
- org.junit.platform:junit-platform-engine
- org.junit.platform:junit-platform-launcher
- org.assertj:assertj-core
- org.apiguardian:apiguardian-api
- org.mockito:mockito-core
- org.mockito:mockito-junit-jupiter
- com.google.code.gson:gson
- commons-io:commons-io
- commons-cli:commons-cli

---
### 🛠 Standard Build Method

Build the project with Gradle:

```
gradle build -Prelease
```

If Gradle is not installed on your system, use the Gradle Wrapper:

```
./gradlew build -Prelease
```

---
### 📂 Standard Installation Method

xgradle is typically installed into system directories used by Linux distributions
(Gradle init scripts, Java JARs, Maven POM metadata).

**1.Install the Gradle plugin (xgradle-resolution-plugin) and init script**
```
install -Dm 644 xgradle-resolution-plugin/build/dist/xgradle-resolution-plugin.jar \
  -t /path/to/gradle/xgradle

install -Dm 644 xgradle-resolution-plugin/build/dist/xgradle-resolution-plugin.gradle \
  -t /path/to/gradle/init.d
```

**2.Install the CLI tool (xgradle-cli)**
```
install -Dm 644 xgradle-cli/build/dist/xgradle-cli.jar \
  -t /usr/share/xgradle

install -Dm 755 xgradle-cli/build/dist/xgradle-cli \
  -t /usr/share/xgradle

ln -s /usr/share/xgradle/xgradle-cli \
  /usr/bin/xgradle-cli
```

---
### 📜 License

Apache 2.0 - See [LICENSE](LICENSE)

```
Copyright 2025 BaseALT Ltd

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---
### 🌱 Contributing

**xgradle is a community project. We welcome new members!**

The easiest way to contribute is to submit pull requests and issues to the
Github repository.

You can also write directly to the xgradle  [maintainers](mailto:xeno@altlinux.org).
