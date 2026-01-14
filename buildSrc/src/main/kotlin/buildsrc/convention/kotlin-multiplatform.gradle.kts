package buildsrc.convention

import buildsrc.config.Deps
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    kotlin("multiplatform")

    id("org.jetbrains.kotlinx.kover")

    id("buildsrc.convention.base")
    id("buildsrc.convention.toolchain-jvm")
}

kotlin {
    targets.configureEach {
        compilations.configureEach {
            compilerOptions.configure {
                apiVersion = Deps.Versions.kotlinCompatibility
                languageVersion = Deps.Versions.kotlinCompatibility
                // Suppress Beta warning for expect/actual classes
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
    targets.withType<KotlinJvmTarget>().configureEach {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaGenerate)
    archiveClassifier.set("javadoc")
}
