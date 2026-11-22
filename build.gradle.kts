// Top-level build file: plugins shared by the modules, plus the project-wide code checks.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kover)
    id("jvm-toolchains")
}

// ---------------------------------------------------------------------------------------
// Code checks
//
//   ./gradlew ktlintCheck     code style (ktlintFormat fixes most of it automatically)
//   ./gradlew detekt          code smells and complexity
//   ./gradlew koverHtmlReport test coverage of the pure-Kotlin modules
//   ./gradlew koverVerify     fails if that coverage drops below the minimum
//
// ktlint and detekt run as plain command-line tools, so they do not depend on which
// Android Gradle Plugin version the project uses.
// ---------------------------------------------------------------------------------------

val ktlintCli: Configuration = configurations.create("ktlintCli")
val detektCli: Configuration = configurations.create("detektCli")

/** Minimum line coverage of the pure-Kotlin modules, checked by koverVerify. */
val minCoveragePercent = 80

/** Modules whose test coverage is measured: all the pure-Kotlin logic and the server. */
val coveredModules = listOf(":tracing", ":model", ":telematics", ":domain", ":simulator", ":server")

dependencies {
    ktlintCli(libs.ktlint.cli) {
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL)) }
    }
    detektCli(libs.detekt.cli)
    coveredModules.forEach { kover(project(it)) }
}

val toolLauncher = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) }
val kotlinFiles = listOf("**/src/**/*.kt", "**/*.gradle.kts", "!**/build/**")

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Checks the Kotlin code style with ktlint."
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    javaLauncher.set(toolLauncher)
    args(kotlinFiles + "--relative")
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fixes the Kotlin code style where ktlint can do it automatically."
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    javaLauncher.set(toolLauncher)
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args(listOf("-F") + kotlinFiles + "--relative")
}

val detektInputs = listOf("app", "tracing", "model", "telematics", "domain", "simulator")
    .flatMap { module -> listOf("main", "test", "androidTest").map { "$module/src/$it" } }
    .filter { file(it).exists() }

tasks.register<JavaExec>("detekt") {
    group = "verification"
    description = "Looks for code smells and too complex code with detekt."
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    javaLauncher.set(toolLauncher)
    args(
        "--input", detektInputs.joinToString(","),
        "--config", "config/detekt/detekt.yml",
        "--build-upon-default-config",
        "--report", "html:build/reports/detekt/detekt.html",
        "--report", "txt:build/reports/detekt/detekt.txt",
    )
}

kover {
    reports {
        verify {
            rule("Line coverage of the pure-Kotlin modules") {
                minBound(minCoveragePercent)
            }
        }
    }
}
