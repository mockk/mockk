package buildsrc.convention

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("buildsrc.convention.android-library")

    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.allopen")

    id("org.jetbrains.dokka")

    id("buildsrc.convention.base")
}

android {
//    compileSdkVersion = "android-32"
//
//    lint {
//        abortOnError = false
//        disable += "InvalidPackage"
//        warning += "NewApi"
//    }
//
//    defaultConfig {
//        minSdk = 26
//        targetSdk = 32
//        version = project.version
//        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
//        testInstrumentationRunnerArguments["notAnnotation"] = "io.mockk.test.SkipInstrumentedAndroidTest"
//
//        ndk {
//            abiFilters += listOf("armeabi-v7a", "x86", "x86_64", "arm64-v8a")
//        }
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_1_8
//        targetCompatibility = JavaVersion.VERSION_1_8
//    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

//    sourceSets {
//        getByName("main") {
//            java.srcDir("src/main/kotlin")
//        }
//        getByName("test") {
//            java.srcDir("src/test/kotlin")
//        }
//    }
}

//kotlin {
//    jvmToolchain {
//        languageVersion.set(JavaLanguageVersion.of("8"))
//    }
//}
//
////java {
////    withJavadocJar()
////    withSourcesJar()
////}
//
//tasks.withType<JavaCompile>().configureEach {
//    options.encoding = "UTF-8"
//}
//
//tasks.withType<KotlinCompile>().configureEach {
//    kotlinOptions.apply {
//        jvmTarget = "1.8"
//        freeCompilerArgs += listOf("-Xjsr305=strict")
//        apiVersion = "1.5"
//        languageVersion = "1.7"
//    }
//}

//tasks.withType<Test>().configureEach {
//    useJUnitPlatform()
//    systemProperties = mapOf(
//        "junit.jupiter.execution.parallel.enabled" to true,
//        "junit.jupiter.execution.parallel.mode.default" to "concurrent",
//        "junit.jupiter.execution.parallel.mode.classes.default" to "concurrent"
//    )
//}

//tasks.named<Jar>("javadocJar") {
//    from(tasks.dokkaJavadoc)
//}
