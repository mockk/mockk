import io.mockk.dependencies.Deps
import io.mockk.dependencies.kotlinVersion

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("dependencies")
}

group = "io.mockk.plugins"
version = "SNAPSHOT"

// Required since Gradle 4.10+.
repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(Deps.Plugins.androidTools)
    implementation(Deps.Plugins.dokka)
    implementation(Deps.Plugins.kotlin(kotlinVersion()))
    implementation("io.mockk.plugins:dependencies:SNAPSHOT")
}

gradlePlugin {
    plugins {
        register("mpp-android") {
            id = "mpp-android"
            implementationClass = "io.mockk.configuration.AndroidConfigurationPlugin"
        }
        register("mpp-jvm") {
            id = "mpp-jvm"
            implementationClass = "io.mockk.configuration.JvmConfigurationPlugin"
        }
    }
}
