package buildsrc.convention

import buildsrc.config.Deps
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    `java-library`
    kotlin("jvm")
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

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(Deps.Versions.jvmTarget.toString()))

        freeCompilerArgs.add("-Xjsr305=strict")
        apiVersion = Deps.Versions.kotlinCompatibility
        languageVersion = Deps.Versions.kotlinCompatibility
    }
}

tasks.named<Jar>("javadocJar") {
    from(tasks.dokkaGenerate)
}
