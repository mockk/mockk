package buildsrc.convention

import buildsrc.config.Deps
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.application")
    kotlin("android")

    id("org.jetbrains.kotlinx.kover")

    id("buildsrc.convention.base")
}

dependencies {
    // Don't add kotlin to dex, only needed for generating dokka
    compileOnly(kotlin("stdlib"))
}

android {
    compileSdk = Deps.Versions.ANDROID_SDK

    lint {
        abortOnError = false
        disable += "InvalidPackage"
        warning += "NewApi"
    }

    packaging {
        resources {
            excludes += "META-INF/main.kotlin_module"
        }
    }

    defaultConfig {
        minSdk = Deps.Versions.ANDROID_MIN_SDK
        targetSdk = Deps.Versions.ANDROID_SDK
    }

    compileOptions {
        sourceCompatibility = Deps.Versions.jvmTarget
        targetCompatibility = Deps.Versions.jvmTarget
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(Deps.Versions.jvmTarget.toString()))
    }
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaGenerate)
    archiveClassifier.set("javadoc")
}
