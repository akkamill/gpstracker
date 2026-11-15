import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// A simulated in-car telematics tracker (Teltonika style). Pure Kotlin, no Android,
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
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
