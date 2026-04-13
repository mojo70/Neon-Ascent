pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ML Kit GenAI Early Access Repository
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
    }
}

rootProject.name = "Neon Ascent"
include(":app")
include(":core:ai-litert")
