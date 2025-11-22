package buildsrc.convention

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")

    id("buildsrc.convention.base")
    id("buildsrc.convention.toolchain-jvm")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.findVersion("java").get().toString())

        apiVersion = KotlinVersion.fromVersion(libs.findVersion("min-kotlin").get().toString())
        languageVersion = KotlinVersion.fromVersion(libs.findVersion("min-kotlin").get().toString())

        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.named<Jar>("javadocJar") {
    from(tasks.dokkaGenerate)
}
