import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl` version "2.4.0"
    `java-gradle-plugin`
}

group = "io.mockk.plugins"
version = "SNAPSHOT"

// Required since Gradle 4.10+.
repositories {
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins.register("dependencies") {
        id = "dependencies"
        implementationClass = "io.mockk.dependencies.DependenciesPlugin"
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
    }
}
