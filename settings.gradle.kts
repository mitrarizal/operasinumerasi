pluginManagement {
    plugins {
        id("com.android.application") version "8.5.0"
        id("com.google.gms.google-services") version "4.3.15"
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "com.bilangannumerasiapp"
include(":app")
