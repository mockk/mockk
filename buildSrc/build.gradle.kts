import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
    // Gradle uses an embedded Kotlin. This doesn't affect the version of Kotlin used to build MockK.
    // https://docs.gradle.org/current/userguide/compatibility.html#kotlin

    idea
}

// set the versions of Gradle plugins that the subprojects will use here
val kotlinPluginVersion: String = "1.7.10"

val androidGradle = "7.2.1"
val byteBuddy = "1.12.10"
val kotlinxCoroutines = "1.6.4"
val dexMaker = "2.28.1"
val objenesis = "3.2"
val objenesisAndroid = "3.2"
val dokka = "1.7.10"
val binaryCompatibilityValidator = "0.11.0"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinPluginVersion"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinPluginVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-allopen")


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

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
