import buildsrc.config.Deps
import buildsrc.config.kotlinVersion

plugins {
    buildsrc.convention.`android-library`
}

extra["mavenName"] = "MockK Android"
description = "mocking library for Kotlin (Android instrumented test)"

//apply(from = "${rootProject.extensions.extraProperties["gradles"]}/upload.gradle")

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
    implementation(projects.modules.mockk)
    implementation(projects.modules.mockkAgentApi)
    implementation(projects.modules.mockkAgentAndroid)

    testImplementation("junit:junit:${Deps.Versions.junit4}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Deps.Versions.androidxEspresso}") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    androidTestImplementation(Deps.Libs.kotlinReflect())
    androidTestImplementation(Deps.Libs.kotlinCoroutinesCore())
    androidTestImplementation(Deps.Libs.kotlinTestJunit()) {
        exclude(group = "junit", module = "junit")
    }
    androidTestImplementation("androidx.test:rules:${Deps.Versions.androidxTestRules}")

    androidTestImplementation(Deps.Libs.junitJupiterApi)
    androidTestImplementation(Deps.Libs.junitJupiterEngine)
    androidTestImplementation(Deps.Libs.junitVintageEngine)
}
