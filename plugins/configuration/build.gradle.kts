import io.mockk.dependencies.Deps
import io.mockk.dependencies.kotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl` version "2.4.0"
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
        register("mpp-common") {
            id = "mpp-common"
            implementationClass = "io.mockk.configuration.CommonConfigurationPlugin"
        }
        register("mpp-android") {
            id = "mpp-android"
            implementationClass = "io.mockk.configuration.AndroidConfigurationPlugin"
        }
        register("mpp-js") {
            id = "mpp-js"
            implementationClass = "io.mockk.configuration.JsConfigurationPlugin"
        }
        register("mpp-jvm") {
            id = "mpp-jvm"
            implementationClass = "io.mockk.configuration.JvmConfigurationPlugin"
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}
