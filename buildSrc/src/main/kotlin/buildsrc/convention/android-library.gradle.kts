package buildsrc.convention

import buildsrc.config.Deps

plugins {
    id("com.android.library")

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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["notAnnotation"] = "io.mockk.test.SkipInstrumentedAndroidTest"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    testImplementation("junit:junit:${Deps.Versions.junit4}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Deps.Versions.androidxEspresso}") {
        exclude("com.android.support:support-annotations")
    }

    androidTestImplementation("androidx.test:rules:${Deps.Versions.androidxTestRules}")
    androidTestImplementation("androidx.test:runner:${Deps.Versions.androidxTestRunner}")
    androidTestImplementation("androidx.test.ext:junit-ktx:${Deps.Versions.androidxTestExtJunit}")

    androidTestImplementation(kotlin("test"))
    androidTestImplementation(kotlin("test-junit"))
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
}
