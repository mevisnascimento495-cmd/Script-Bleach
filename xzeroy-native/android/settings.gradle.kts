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
        maven("https://raw.githubusercontent.com/SkidderMC/ProtoHax-PackageGit/main/maven_repo/")
    }
}
rootProject.name = "XZeroyNative"
include(":app")

// The app consumes the published shaded ProtoHax 1.4.0 artifact below.
// The source submodule remains pinned for audit and license compliance; its
// archived composite build has transitive dependencies that are no longer hosted.
