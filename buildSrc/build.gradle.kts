import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.6.21"
    // Gradle uses an embedded Kotlin with version 1.4
    // https://docs.gradle.org/current/userguide/compatibility.html#kotlin
    // but it's safe to use 1.6.21, as long as the language level is set to 1.4
    // (the kotlin-dsl plugin does this).
}

// set the versions of Gradle plugins that the subprojects will use here
val kotlinPluginVersion: String = "1.7.10"

val androidGradle = "7.2.1"
val byteBuddy = "1.12.10"
val kotlinxCoroutines = "1.6.4"
val dexMaker = "2.28.1"
val objenesis = "3.2"
val objenesisAndroid = "3.2"
//val junitJupiter = "5.8.2"
//val junitVintage = "5.8.2"
val dokka = "1.7.10"
val binaryCompatibilityValidator = "0.11.0"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinPluginVersion"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinPluginVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect")


    implementation("com.android.tools.build:gradle:$androidGradle")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokka")

    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:$binaryCompatibilityValidator")
//    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(8))
    }

    kotlinDslPluginOptions {
        jvmTarget.set("1.8")
    }
}
