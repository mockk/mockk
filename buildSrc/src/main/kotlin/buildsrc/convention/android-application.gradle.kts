package buildsrc.convention

import buildsrc.config.Deps

plugins {
    id("com.android.application")

    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")

    id("buildsrc.convention.base")
}

android {
    compileSdkVersion = "android-32"

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
