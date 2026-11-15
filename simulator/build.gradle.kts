import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// A simulated car with an in-car vehicle tracker. Pure Kotlin, no Android,
// so the app, the server and a command-line tool can all run the same fake car.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    api(project(":telematics"))
    api(libs.kotlinx.coroutines.core)
    implementation(project(":tracing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
