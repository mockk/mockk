package buildsrc.convention

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    kotlin("multiplatform")

    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")

    id("buildsrc.convention.base")
    id("buildsrc.convention.toolchain-jvm")
}

kotlin {
    targets.configureEach {
        compilations.configureEach {
            compilerOptions.configure {
                apiVersion = KotlinVersion.KOTLIN_1_6
                languageVersion = KotlinVersion.KOTLIN_1_7
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
    from(tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
}
