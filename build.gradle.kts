// Root project build configuration
// Repositories and common plugins are defined here
// Subprojects inherit this configuration

plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// Common configuration for all subprojects
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    kotlin {
        jvmToolchain(21)
    }
}