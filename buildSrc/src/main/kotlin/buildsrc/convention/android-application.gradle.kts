package buildsrc.convention

import buildsrc.config.Deps
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")

    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")

    id("buildsrc.convention.base")
}

android {
    compileSdk = Deps.Versions.compileSdk

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
        minSdk = Deps.Versions.minSdk
        targetSdk = Deps.Versions.targetSdk
    }

    compileOptions {
        sourceCompatibility = Deps.Versions.jvmTarget
        targetCompatibility = Deps.Versions.jvmTarget
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Deps.Versions.jvmTarget.toString()
    }
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
}
