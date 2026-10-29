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
    }
}
rootProject.name = "XZeroyNative"
include(":app")

// ProtoHax sources are compiled as part of the app; its required libraries are declared in app/build.gradle.kts.
