import buildsrc.config.Deps

plugins {
    buildsrc.convention.`android-library`

    buildsrc.convention.`mockk-publishing`
}

description = "Mocking library for Kotlin (Android instrumented test)"

val mavenName: String by extra("MockK Android")
val mavenDescription: String by extra("${project.description}")

@Suppress("UnstableApiUsage")
android {
    packagingOptions {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }

    defaultConfig {
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["notAnnotation"] = "io.mockk.test.SkipInstrumentedAndroidTest"
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/common/src/test/kotlin")
    }
}

dependencies {
    api(projects.modules.mockk)
    api(projects.modules.mockkAgentApi)
    api(projects.modules.mockkAgentAndroid)

    testImplementation("junit:junit:${Deps.Versions.junit4}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Deps.Versions.androidxEspresso}") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    androidTestImplementation(kotlin("reflect"))

    androidTestImplementation("androidx.test:rules:${Deps.Versions.androidxTestRules}")

    androidTestImplementation(kotlin("test"))
    androidTestImplementation(kotlin("test-junit"))
    androidTestImplementation(Deps.Libs.junitJupiter)
    androidTestImplementation(Deps.Libs.junitVintageEngine)
}
