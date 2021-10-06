plugins {
    `kotlin-dsl`
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
