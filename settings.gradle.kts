plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "mcp-davx5"

// Include server subproject
include("server")

// for debugging
/*includeBuild("../dav4jvm") {
    dependencySubstitution {
        substitute(module("com.github.bitfireat:dav4jvm")).using(project(":server"))
    }
}*/