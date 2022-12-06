package buildsrc.convention

import buildsrc.config.Deps
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")

    id("buildsrc.convention.base")
    id("buildsrc.convention.toolchain-jvm")
}

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
        jvmTarget = Deps.Versions.jvmTarget.toString()
        apiVersion = "1.5"
        languageVersion = "1.7"
    }
}

tasks.named<Jar>("javadocJar") {
    from(tasks.dokkaJavadoc)
}
