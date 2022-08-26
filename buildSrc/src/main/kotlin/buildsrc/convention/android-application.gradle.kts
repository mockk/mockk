package buildsrc.convention

import buildsrc.config.Deps
import org.gradle.jvm.tasks.Jar

plugins {
    id("com.android.application")

    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.allopen")

    id("org.jetbrains.dokka")

    id("buildsrc.convention.base")
}

android {
    compileSdkVersion = "android-32"

    kotlinOptions {
        jvmTarget = Deps.Versions.jvmTarget.toString()
    }

    lint {
        abortOnError = false
        disable += "InvalidPackage"
        warning += "NewApi"
    }

    packagingOptions {
        resources {
            excludes += "META-INF/main.kotlin_module"
        }
    }

    defaultConfig {
        minSdk = 21
        targetSdk = 32
    }

    compileOptions {
        sourceCompatibility = Deps.Versions.jvmTarget
        targetCompatibility = Deps.Versions.jvmTarget
    }
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
}
