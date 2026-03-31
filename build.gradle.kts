plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.sqldelight)
}

group = "at.bitfire.labs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

kotlin {
    jvmToolchain(21)
}

sqldelight {
    databases {
        register("Database") {
            packageName.set("at.bitfire.labs.davmcp.db")
        }
    }
}

application {
    mainClass.set("at.bitfire.labs.davmcp.MainKt")
}

dependencies {
    implementation(libs.dav4jvm)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.server)
    implementation(libs.mcp.kotlin.sdk)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ical4j)
    implementation(libs.dagger)
    implementation(libs.sqldelight.sqlite.driver)
    implementation(libs.slf4j.jdk14)
    ksp(libs.dagger.compiler)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    mustRunAfter(tasks.jar)
    
    manifest {
        attributes["Main-Class"] = "at.bitfire.labs.davmcp.MainKt"
    }

    from(
        sourceSets.main.get().output
    )
    
    from(
        configurations.runtimeClasspath.get().map { 
            if (it.isDirectory) it else zipTree(it)
        }
    )
}