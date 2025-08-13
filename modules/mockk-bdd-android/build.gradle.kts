plugins {
    buildsrc.convention.`android-library`
    buildsrc.convention.`mockk-publishing`
}

description = "MockK BDD style aliases for Android"

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