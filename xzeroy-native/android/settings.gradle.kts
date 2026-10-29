pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://repo.opencollab.dev/maven-snapshots")
        maven("https://repo.opencollab.dev/maven-releases")
        maven("https://repo.genshit.xyz/maven_repo/")
        maven("https://mccheatz.github.io/maven_repo/") // legacy package host
    }
}
rootProject.name = "XZeroyNative"
include(":app")

// Build the GPL-licensed Bedrock relay as a separate composite build.
includeBuild("../engine/protohax")
