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
        // Hosts transitive ProtoHax dependencies such as minecraft-msftauth; the ProtoHax engine itself is compiled from the pinned source.
        maven("https://raw.githubusercontent.com/SkidderMC/ProtoHax-PackageGit/main/maven_repo/")
    }
}
rootProject.name = "XZeroyNative"
include(":app")

// ProtoHax sources are compiled as part of the app; its required libraries are declared in app/build.gradle.kts.
