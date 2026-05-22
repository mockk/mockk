package buildsrc.convention

import buildsrc.config.Deps
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.library")

    kotlin("android")

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
        // AGP 8.5 changed the default of jniLibs.useLegacyPackaging from true to false for test
        // APKs, which leaves libmockkjvmtiagent.so packed inside the APK rather than extracted to
        // the filesystem. Debug.attachJvmtiAgent can no longer locate it by bare name in that
        // case, and the JvmtiAgent runtime fallback extracts it on demand, but applying legacy
        // packaging here keeps mockk's own instrumented tests on the well-trodden path.
        packaging {
            jniLibs {
                useLegacyPackaging = true
            }
        }
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
    from(tasks.dokkaGenerate)
    archiveClassifier.set("javadoc")
}
