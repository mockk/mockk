import io.mockk.dependencies.Deps

plugins {
    id("mpp-android")
}

extra["mavenName"] = "MockK Android"
extra["mavenDescription"] = "mocking library for Kotlin (Android instrumented test)"

apply(from = "${rootProject.extensions.extraProperties["gradles"]}/upload.gradle")

android {
    compileSdkVersion("android-32")


    lintOptions {
        isAbortOnError = false
        disable("InvalidPackage")
        warning("NewApi")
    }

    packagingOptions {
        exclude("META-INF/main.kotlin_module")
        exclude("META-INF/LICENSE.md")
        exclude("META-INF/LICENSE-notice.md")
    }

    defaultConfig {
        minSdk = 21
        targetSdk = 32
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments
        testInstrumentationRunnerArguments["notAnnotation"] = "io.mockk.test.SkipInstrumentedAndroidTest"
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/common/src/test/kotlin")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

// very weird hack to make it working in IDE (check settings.gradle)
val mockKProject = findProject(":mockk-jvm")?.project ?: project(":mockk")

dependencies {
    api(project(":${mockKProject.name}")) {
        exclude(group = "io.mockk", module = "mockk-agent-jvm")
    }
    implementation(project(":mockk-agent-android"))
    implementation(project(":mockk-agent-api"))

    testImplementation(Testing.junit4)
    androidTestImplementation("androidx.test.espresso:espresso-core:_") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    androidTestImplementation("org.jetbrains.kotlin:kotlin-reflect:_")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:_")
    androidTestImplementation("org.jetbrains.kotlin:kotlin-test-junit:_") {
        exclude(group = "junit", module = "junit")
    }
    androidTestImplementation(AndroidX.test.rules)

    androidTestImplementation(Deps.Libs.junitJupiterApi)
    androidTestImplementation(Deps.Libs.junitJupiterEngine)
    androidTestImplementation(Deps.Libs.junitVintageEngine)
}
