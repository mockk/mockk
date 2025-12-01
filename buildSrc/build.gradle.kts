plugins {
    `kotlin-dsl`
}

// set the versions of Gradle plugins that the subprojects will use here
val kotlinPluginVersion: String = "2.1.20"

val androidGradle = "8.9.2"
val kotlinxKover = "0.9.1"
val dokka = "2.0.0"
val binaryCompatibilityValidator = "0.17.0"
val jreleaserVersion = "1.17.0"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinPluginVersion"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinPluginVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinPluginVersion")

    implementation("org.jetbrains.kotlinx.kover:org.jetbrains.kotlinx.kover.gradle.plugin:$kotlinxKover")

    // Temporarily commented out due to network restrictions in sandbox
    // implementation("com.android.tools.build:gradle:$androidGradle")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokka")

    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:$binaryCompatibilityValidator")


    implementation("org.jreleaser:jreleaser-gradle-plugin:$jreleaserVersion")
}
