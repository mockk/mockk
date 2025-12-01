// A settings.gradle.kts plugin for defining shared repositories used by both buildSrc and the root project

@Suppress("UnstableApiUsage") // Central declaration of repositories is an incubating feature
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }

    pluginManagement {
        repositories {
            gradlePluginPortal()
            mavenCentral()
            google()
        }
    }
}
