pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SegmentAnalyzer"

include(
    ":app",
    ":core",
    ":common",
    ":domain",
    ":data",
    ":feature-history",
    ":feature-import",
    ":feature-analysis",
    ":feature-segments",
    ":feature-settings",
    ":feature-auth",
)
