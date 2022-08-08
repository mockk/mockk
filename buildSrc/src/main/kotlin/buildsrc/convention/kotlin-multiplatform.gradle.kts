package buildsrc.convention

import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    kotlin("multiplatform")

    id("org.jetbrains.dokka")

    id("buildsrc.convention.base")
}

kotlin {
    targets.configureEach {
        compilations.configureEach {
            kotlinOptions {
                apiVersion = "1.5"
                languageVersion = "1.7"
            }
        }
    }
    targets.withType<KotlinJvmTarget>().configureEach {
        val toolchainJavaVersion = providers.gradleProperty("toolchainJavaVersion")
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(toolchainJavaVersion.get()))
        }

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
}

val testToolchainJavaVersion = providers.gradleProperty("testToolchainJavaVersion")

tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(testToolchainJavaVersion.get()))
    })
}
