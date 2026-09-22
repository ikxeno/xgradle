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
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.language.jvm.tasks.ProcessResources
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private fun Project.gitCommitIdProvider(): Provider<String> =
    providers.systemProperty("gitCommitId")
        .orElse(providers.provider { git("rev-parse", "HEAD~0") })
        .orElse("<Unknown>")
        .map(String::trim)
        .filter(String::isNotBlank)

private fun Project.git(vararg args: String): String? =
    runCatching {
        val p = ProcessBuilder("git", *args)
            .directory(rootProject.projectDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()

        p.waitFor(10, TimeUnit.SECONDS)
        if (p.exitValue() == 0) p.inputStream.bufferedReader().use { it.readText().trim() } else null
    }.getOrNull()

private fun buildTime(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(Date())

tasks.withType<ProcessResources>().configureEach {
    filteringCharset = "UTF-8"

    when (project.path) {
        ":xgradle-resolution-plugin" -> {
            val version = rootProject.version.toString()

            filesMatching("logo.txt") {
                filter { line -> line.replace("@version@", version) }
            }
        }

        ":xgradle-cli" -> {
            val projectVersion = project.version.toString()
            val commitId = project.gitCommitIdProvider()
            val time = buildTime()

            filesMatching("application.properties") {
                expand(
                    "projectVersion" to projectVersion,
                    "gitCommitHash" to commitId.get(),
                    "projectBuildTime" to time
                )
            }
        }
    }
}
