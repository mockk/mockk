package buildsrc.convention

import buildsrc.config.Deps
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.library")

    kotlin("android")

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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    compileOptions {
        sourceCompatibility = Deps.Versions.jvmTarget
        targetCompatibility = Deps.Versions.jvmTarget
    }

    publishing {
        singleVariant("release") {}
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(Deps.Versions.jvmTarget.toString()))
    }
}

dependencies {
    androidTestImplementation(kotlin("test"))
    androidTestImplementation(kotlin("test-junit"))

    androidTestImplementation("androidx.test.espresso:espresso-core:${Deps.Versions.ANDROID_TEST_ESPRESSO}")
    androidTestImplementation("androidx.test:rules:${Deps.Versions.ANDROID_TEST_RULES}")
    androidTestImplementation("androidx.test:runner:${Deps.Versions.ANDROID_TEST_RUNNER}")
    androidTestImplementation("androidx.test.ext:junit-ktx:${Deps.Versions.ANDROID_TEST_JUNIT_EXTENSIONS}")
    androidTestUtil("androidx.test:orchestrator:${Deps.Versions.ANDROID_TEST_ORCHESTRATOR}")
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
}
