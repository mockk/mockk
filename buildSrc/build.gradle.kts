plugins {
    `kotlin-dsl`
}

// defines versions for Gradle plugins in convention plugins
dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.reflect)

    implementation(libs.kover)
    implementation(libs.android.gradle)
    implementation(libs.dokka)
    implementation(libs.binary.compatibility.validator)
    implementation(libs.jreleaser)
}
