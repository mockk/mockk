import buildsrc.config.Deps

plugins {
    buildsrc.convention.`android-library`

    buildsrc.convention.`mockk-publishing`
}

description = "Mocking library for Kotlin (Android instrumented test)"

val mavenName: String by extra("MockK Android")
val mavenDescription: String by extra("${project.description}")

android {
    packagingOptions {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    implementation(projects.modules.mockk)
    implementation(projects.modules.mockkAgentApi)
    implementation(projects.modules.mockkAgentAndroid)

    implementation(platform(Deps.Libs.kotlinCoroutinesBom))
    implementation(Deps.Libs.kotlinCoroutinesCore)
}
