plugins {
    `kotlin-dsl`
}

// set the versions of Gradle plugins that the subprojects will use here

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.reflect)
    implementation(libs.android.gradle)

    implementation(libs.dokka)
    implementation(libs.kover)
    implementation(libs.binary.compatibility.validator)
    implementation(libs.jreleaser)
}
