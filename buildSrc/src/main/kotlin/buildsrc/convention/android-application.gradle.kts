package buildsrc.convention

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
        jvmTarget = JavaVersion.VERSION_1_8.toString()
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
        minSdk = 26
        targetSdk = 32
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
