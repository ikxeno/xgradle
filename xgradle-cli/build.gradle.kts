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
plugins {
    application
    id("org.altlinux.xgradle-publishing-conventions")
}

dependencies {
    implementation(libs.bundles.maven.tooling)
    implementation(libs.bundles.logging)
    implementation(libs.jcommander)
    implementation(libs.plexus.utils)
    implementation(libs.guice)
    runtimeOnly(libs.bundles.guice.deps)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.bundles.testing.platform)

    shadow(libs.bundles.maven.tooling)
    shadow(libs.bundles.logging)
    shadow(libs.jcommander)
    shadow(libs.plexus.utils)
    shadow(libs.guice)
    shadow(libs.bundles.guice.deps)
}

apply(from = rootProject.file("version.gradle.kts"))

application {
    mainClass.set("${project.group}.impl.Main")
}

tasks.shadowJar {

    configurations = listOf(project.configurations.shadow.get())

    minimize()
    isEnableRelocation = true

    archiveBaseName.set(project.name)
    archiveClassifier.set(null as String?)

    manifest{
        attributes(
            "Main-Class" to "${project.group}.impl.Main",
            "Implementation-Version" to project.version
        )
    }
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

tasks.register<Task>("createShellScript") {
    dependsOn("shadowJar")

    val outputDir = layout.buildDirectory.dir("dist")
    val scriptFile = outputDir.get().file(project.name).asFile

    val launcher = layout.projectDirectory.file("src/main/sh/launcher.sh")
    inputs.file(launcher)
    outputs.file(scriptFile)

    doLast {
        val scriptContent = launcher.asFile.readText().replace("@JAR@", "${project.name}.jar")

        scriptFile.parentFile.mkdirs()
        scriptFile.writeText(scriptContent)
        scriptFile.setExecutable(true)
    }
}

tasks.matching { it.name in listOf("distZip", "distTar", "startScripts") }
    .configureEach {
        dependsOn(tasks.named("shadowJar"))
    }

xgradlePublishingConventions {
    projectName.set("XGradle Tool")
    projectDescription.set("${rootProject.name} support tool")
    projectUrl.set("https://altlinux.space/ALTLinux/xgradle.git")

    licenseName.set("The Apache License, Version 2.0")
    licenseUrl.set("https://www.apache.org/licenses/LICENSE-2.0")

    enableCopyPublications.set(true)

    withShadowJar()
    withSourcesJar()
    withJavadocJar()

    developer("xeno", "Ivan Khanas", "xeno@altlinux.org")
}

tasks.named("publishToMavenLocal") {
    dependsOn("createShellScript")
}

tasks.named("build") {
    dependsOn("createShellScript")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        showStackTraces = true
    }
}