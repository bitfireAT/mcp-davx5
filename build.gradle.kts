plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    application
}

group = "at.bitfire.labs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(libs.dav4jvm)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.server)
    implementation(libs.mcp.kotlin.sdk)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ical4j)
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("at.bitfire.labs.davmcp.MainKt")
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
}