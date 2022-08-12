package buildsrc.convention

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")

    id("buildsrc.convention.base")
    id("buildsrc.convention.toolchain-jvm")
}

// note: all subprojects are currently Kotlin Multiplatform, so this convention plugin is unused

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.apply {
        freeCompilerArgs += listOf("-Xjsr305=strict")
        apiVersion = "1.5"
        languageVersion = "1.7"
    }
}

tasks.named<Jar>("javadocJar") {
    from(tasks.dokkaJavadoc)
}
