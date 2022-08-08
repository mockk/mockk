package buildsrc.convention

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")

    id("buildsrc.convention.base")
}

// note: all subprojects are currently Kotlin Multiplatform, so this convention plugin is unused

val toolchainJavaVersion = providers.gradleProperty("toolchainJavaVersion")

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(toolchainJavaVersion.get()))
    }
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
        jvmTarget = "1.8"
        freeCompilerArgs += listOf("-Xjsr305=strict")
        apiVersion = "1.5"
        languageVersion = "1.7"
    }
}

tasks.named<Jar>("javadocJar") {
    from(tasks.dokkaJavadoc)
}

val testToolchainJavaVersion = providers.gradleProperty("testToolchainJavaVersion")

tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(testToolchainJavaVersion.get()))
    })
}
