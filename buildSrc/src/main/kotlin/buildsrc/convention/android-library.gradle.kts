package buildsrc.convention

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.library")

    kotlin("android")

    id("org.jetbrains.kotlinx.kover")

    id("buildsrc.convention.base")
    id("buildsrc.convention.dokka")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

android {
    compileSdk = libs.findVersion("sdk").get().toString().toInt()

    lint {
        targetSdk = libs.findVersion("sdk").get().toString().toInt()
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
        minSdk = libs.findVersion("min-sdk").get().toString().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        targetSdk = libs.findVersion("sdk").get().toString().toInt()
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.findVersion("java").get().toString())
        targetCompatibility = JavaVersion.toVersion(libs.findVersion("java").get().toString())
    }

    publishing {
        singleVariant("release") {}
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.findVersion("java").get().toString()))
    }
}

dependencies {
    testImplementation(libs.findLibrary("junit4").get())
    androidTestImplementation(libs.findLibrary("androidx-espresso").get())

    androidTestImplementation(libs.findLibrary("androidx-rules").get())
    androidTestImplementation(libs.findLibrary("androidx-runner").get())
    androidTestImplementation(libs.findLibrary("androidx-junit").get())
    androidTestUtil(libs.findLibrary("androidx-orchestrator").get())

    androidTestImplementation(kotlin("test"))
    androidTestImplementation(kotlin("test-junit"))
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaGenerate)
    archiveClassifier.set("javadoc")
}
