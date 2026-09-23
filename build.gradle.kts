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
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    `maven-publish`
    alias(libs.plugins.shadow) apply false
}

allprojects {
    group = property("group") as String
    version = if(rootProject.hasProperty("release"))
    {
        property("version") as String
    } else {
        property("version") as String + "-SNAPSHOT"
    }
}

subprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "com.gradleup.shadow")

    if (!gradle.startParameter.isOffline) {
        apply(plugin = "checkstyle")
        apply(plugin = "jacoco")

        configure<CheckstyleExtension> {
            isIgnoreFailures = false
            isShowViolations = true
            maxErrors = 0
        }

        tasks.withType<Checkstyle>().configureEach {
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }

        tasks.register("checkstyle") {
            dependsOn("checkstyleMain", "checkstyleTest")
        }

        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.check)
            reports {
                xml.required.set(true)
                html.required.set(true)
                csv.required.set(false)
            }
        }
    }

    java {
	// Please make sure that all dependencies in your distribution have Class file version 55.0 or lower.
        if (providers.systemProperty("java11").isPresent){
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }else {
            targetCompatibility = JavaVersion.VERSION_17
            sourceCompatibility = JavaVersion.VERSION_17
        }
    }
}
