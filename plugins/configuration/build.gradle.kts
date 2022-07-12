import io.mockk.dependencies.Deps
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    implementation("com.android.tools.build:gradle:7.2.1")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.7.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
    implementation("io.mockk.plugins:dependencies:_")
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
