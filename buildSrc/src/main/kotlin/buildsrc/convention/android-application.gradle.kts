package buildsrc.convention

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.application")

    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")

    id("buildsrc.convention.base")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

android {
    compileSdk = libs.findVersion("sdk").get().toString().toInt()

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
        minSdk = libs.findVersion("min-sdk").get().toString().toInt()
        targetSdk = libs.findVersion("sdk").get().toString().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.findVersion("java").get().toString())
        targetCompatibility = JavaVersion.toVersion(libs.findVersion("java").get().toString())
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.findVersion("java").get().toString()))
    }
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaGenerate)
    archiveClassifier.set("javadoc")
}
