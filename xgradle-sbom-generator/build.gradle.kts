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

apply(from = rootProject.file("version.gradle.kts"))

plugins {
    `java-library`
    id("org.altlinux.xgradle-publishing-conventions")
}

dependencies {
    implementation(libs.guice)
    implementation(libs.gson)
    runtimeOnly(libs.bundles.guice.deps)

    shadow(libs.guice)
    shadow(libs.gson)
    shadow(libs.bundles.guice.deps)

    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.bundles.testing.platform)
}

tasks.register<Jar>("sourcesJar") {
    archiveBaseName.set(project.name)
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.register<Jar>("javadocJar") {
    archiveBaseName.set(project.name)
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

xgradlePublishingConventions {
    projectName.set("XGradle SBOM Generator")
    projectDescription.set("${rootProject.name} SBOM generator support module")
    projectUrl.set("https://altlinux.space/ALTLinux/xgradle.git")

    licenseName.set("The Apache License, Version 2.0")
    licenseUrl.set("https://www.apache.org/licenses/LICENSE-2.0")

    enableCopyPublications.set(true)

    withJar()
    withSourcesJar()
    withJavadocJar()

    developer("xeno", "Ivan Khanas", "xeno@altlinux.org")
}

tasks.test {
    dependsOn(":xgradle-resolution-plugin:copyPublicationsToDist")
    mustRunAfter(":xgradle-resolution-plugin:copyPublicationsToDist")
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        showStackTraces = true
    }

    gradle.gradleHomeDir?.let { systemProperty("xgradle.test.gradleHome", it.absolutePath) }
}
