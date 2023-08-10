package buildsrc.convention

import buildsrc.config.Deps

plugins {
    id("com.android.library")

    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.allopen")

    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")

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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    compileOptions {
        sourceCompatibility = Deps.Versions.jvmTarget
        targetCompatibility = Deps.Versions.jvmTarget
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
    androidTestUtil("androidx.test:orchestrator:${Deps.Versions.androidxTestOrchestrator}")

    androidTestImplementation(kotlin("test"))
    androidTestImplementation(kotlin("test-junit"))
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
}
