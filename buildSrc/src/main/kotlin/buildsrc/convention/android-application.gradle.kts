package buildsrc.convention

import buildsrc.config.Deps
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.application")

    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")

    id("buildsrc.convention.base")
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
    from(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
}
