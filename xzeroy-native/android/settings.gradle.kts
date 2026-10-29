pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "XZeroyNative"
include(":app")

// Build the GPL-licensed Bedrock relay as a separate composite build.
includeBuild("../engine/protohax")
