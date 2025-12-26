plugins {
    buildsrc.convention.`android-library`
    buildsrc.convention.`mockk-publishing`
}

description = "MockK BDD style aliases for Android"

// BDD style API for MockK Android.
// This module re-exports the base BDD functions and adds Android-specific extensions.
//
// To use this module in your Android tests:
// ```
// testImplementation("io.mockk:mockk-bdd-android:x.y.z")
// ```

val mavenName: String by extra("MockK BDD Android")
val mavenDescription: String by extra("${project.description}")

android {
    namespace = "io.mockk.bdd.android"
    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    api(projects.modules.mockkAndroid)
    api(projects.modules.mockkBdd)

    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
}
