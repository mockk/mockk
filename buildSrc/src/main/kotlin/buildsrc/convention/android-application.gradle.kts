package buildsrc.convention

plugins {
    id("com.android.application")

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
        exclude("META-INF/main.kotlin_module")
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
