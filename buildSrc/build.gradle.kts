import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

// set the versions of Gradle plugins that the subprojects will use here
val kotlinPluginVersion: String = "1.8.20-RC"

val androidGradle = "7.2.1"
val kotlinxKover = "0.6.1"
val dokka = "1.7.10"
val binaryCompatibilityValidator = "0.11.0"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinPluginVersion"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinPluginVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinPluginVersion")
    implementation("org.jetbrains.kotlin:kotlin-allopen:$kotlinPluginVersion")

    implementation("org.jetbrains.kotlinx:kover:$kotlinxKover")

    implementation("com.android.tools.build:gradle:$androidGradle")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokka")

    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:$binaryCompatibilityValidator")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

kotlinDslPluginOptions {
    jvmTarget.set("11")
}
