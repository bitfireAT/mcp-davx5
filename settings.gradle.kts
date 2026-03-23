plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "3dav-mcp"

// for debugging
/*includeBuild("../dav4jvm") {
    dependencySubstitution {
        substitute(module("com.github.bitfireat:dav4jvm")).using(project(":"))
    }
}*/