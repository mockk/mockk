import buildsrc.config.Deps
import buildsrc.config.kotlinVersion

plugins {
    buildsrc.convention.`kotlin-android`
}

extra["mavenName"] = "MockK Android"
description = "mocking library for Kotlin (Android instrumented test)"

//apply(from = "${rootProject.extensions.extraProperties["gradles"]}/upload.gradle")

android {
    compileSdkVersion("android-31")


    lint {
        abortOnError = false
        disable += "InvalidPackage"
        warning += "NewApi"
    }

    packagingOptions {
        exclude("META-INF/main.kotlin_module")
        exclude("META-INF/LICENSE.md")
        exclude("META-INF/LICENSE-notice.md")
    }

    defaultConfig {
        minSdk = 26
        targetSdk = 32
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["notAnnotation"] = "io.mockk.test.SkipInstrumentedAndroidTest"
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/common/src/test/kotlin")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}

// very weird hack to make it working in IDE (check settings.gradle)
//val mockKProject = findProject(":mockk-jvm")?.project ?: project(":mockk")

dependencies {
//    api(project(":${mockKProject.name}")) {
//        exclude(group = "io.mockk", module = "mockk-agent-jvm")
//    }
    implementation(projects.modules.mockkAgentApi)
    implementation(projects.modules.mockkAgent)
    implementation(projects.modules.mockkAgentAndroid)
//    implementation(project(":modules:mockk-agent-android"))
//    implementation(project(":modules:mockk-agent-api"))

    testImplementation("junit:junit:4.13.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    androidTestImplementation(Deps.Libs.kotlinReflect(kotlinVersion()))
    androidTestImplementation(Deps.Libs.kotlinCoroutinesCore())
    androidTestImplementation(Deps.Libs.kotlinTestJunit()) {
        exclude(group = "junit", module = "junit")
    }
    androidTestImplementation("androidx.test:rules:1.4.0")

    androidTestImplementation(Deps.Libs.junitJupiterApi)
    androidTestImplementation(Deps.Libs.junitJupiterEngine)
    androidTestImplementation(Deps.Libs.junitVintageEngine)
}
