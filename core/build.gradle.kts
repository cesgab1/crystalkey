plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pinned to 17 explicitly rather than via jvmToolchain(17): a toolchain has to
// be *found or downloaded*, and Android Studio commonly runs Gradle on JDK 21,
// which turns into "No matching toolchains found". This compiles with whatever
// JDK Gradle is on and just targets 17 — which is also what :app targets, so
// the two modules stay binary-compatible.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // kotlin("test") is special-cased by the Kotlin plugin: it resolves to the
    // variant matching the configured test framework (JUnit4 here).
    testImplementation(kotlin("test"))
}

tasks.test {
    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped")
    }
}
