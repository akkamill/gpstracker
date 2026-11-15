import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The data format of in-car vehicle trackers (AVL records) and its conversion into
// the app's telemetry model. Used by the simulator, the app and later the server.
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
    api(project(":model"))
    implementation(project(":tracing"))
    testImplementation(libs.junit)
}
