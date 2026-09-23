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
    `java-gradle-plugin`
    id("org.altlinux.xgradle-publishing-conventions")
}

val initScriptName = "${project.name}.gradle"

configurations.shadow {
    exclude(group = "org.slf4j")
}

dependencies {
    compileOnly(gradleApi())
    implementation(project(":xgradle-sbom-generator"))
    implementation(libs.bundles.maven.tooling)
    implementation(libs.guice)
    implementation(libs.guava)
    runtimeOnly(libs.plexus.utils)
    runtimeOnly(libs.bundles.guice.deps)
    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.bundles.testing.platform)

    shadow(project(":xgradle-sbom-generator"))
    shadow(libs.gson)
    shadow(libs.bundles.maven.tooling)
    shadow(libs.plexus.utils)
    shadow(libs.guice)
    shadow(libs.bundles.guice.deps)
}

gradlePlugin{
    isAutomatedPublishing = false
    plugins{
        create(project.name) {
            id = project.group as String
            implementationClass = "${project.group}.impl.plugin.XGradlePlugin"
        }
    }
}

tasks.register<Copy>("copyInitScript") {
    dependsOn("processResources")
    from("src/main/resources/$initScriptName")
    into(layout.buildDirectory.dir("dist"))
}

tasks.shadowJar {
    dependsOn("copyInitScript")
    archiveClassifier.set(null as String?)

    configurations = listOf(project.configurations.shadow.get())

    metaInf {
        from(rootProject.projectDir) {
            include("LICENSE")
            include("NOTICE")
        }
    }

    minimize()

    isEnableRelocation = true
}

tasks.named<Jar>("jar") {
    enabled = false
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
    projectName.set(project.name)
    projectDescription.set("${rootProject.name} plugin for offline build")
    projectUrl.set("https://altlinux.space/ALTLinux/xgradle.git")

    licenseName.set("The Apache License, Version 2.0")
    licenseUrl.set("https://www.apache.org/licenses/LICENSE-2.0")

    enablePluginMarker.set(true)
    enableCopyPublications.set(true)

    withShadowJar()
    withSourcesJar()
    withJavadocJar()

    developer("xeno", "Ivan Khanas", "xeno@altlinux.org")
}

tasks.named("publishToMavenLocal") {
    dependsOn("copyInitScript")
}

tasks.named("build") {
    dependsOn("assemble")
    dependsOn("shadowJar", "javadocJar", "publishToMavenLocal")
}

tasks.test {
    mustRunAfter("copyPublicationsToDist")
    useJUnitPlatform ()

    testLogging {
        events("passed", "skipped", "failed")
        showStackTraces = true
    }

    systemProperty("java.library.dir", System.getProperty("java.library.dir"))
    systemProperty("maven.poms.dir", System.getProperty("maven.poms.dir"))
}

tasks.named("clean") {
    doLast {
        listOf(
            initScriptName,
            "${rootProject.name}-plugin.gradle"
        ).forEach { scriptName ->
            val targetFile = layout.buildDirectory.file(scriptName).get().asFile
            if (targetFile.exists()) {
                targetFile.delete()
            }
        }
    }
}
