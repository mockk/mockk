pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

apply(from = "gradle/detect-android-sdk.gradle")

rootProject.name = "mockk-root"
includeBuild("plugins/dependencies")
includeBuild("plugins/configuration")

include("mockk-jvm")
include("mockk-common")
//include 'mockk-js'

val hasAndroidSdk = extra["hasAndroidSdk"]

if (hasAndroidSdk == true) include("mockk-android")

include("mockk-agent-api")
include("mockk-agent-common")
include("mockk-agent-jvm")
if (hasAndroidSdk == true) {
    include("mockk-agent-android")
    include("mockk-agent-android-dispatcher")
}

include("mockk-dsl")
include("mockk-dsl-jvm")
// include 'mockk-dsl-js'

// include("mockk-client-tests-jvm")

project(":mockk-jvm").projectDir = file("mockk/jvm")
project(":mockk-common").projectDir = file("mockk/common")
//project(":mockk-js").projectDir = file("mockk/js")
if (hasAndroidSdk == true) project(":mockk-android").projectDir = file("mockk/android")

project(":mockk-agent-api").projectDir = file("agent/api")
project(":mockk-agent-common").projectDir = file("agent/common")
project(":mockk-agent-jvm").projectDir = file("agent/jvm")
if (hasAndroidSdk == true) {
    project(":mockk-agent-android").projectDir = file("agent/android")
    project(":mockk-agent-android-dispatcher").projectDir = file("agent/android/dispatcher")
}

project(":mockk-dsl").projectDir = file("dsl/common")
project(":mockk-dsl-jvm").projectDir = file("dsl/jvm")
// project(":mockk-dsl-js").projectDir = file("dsl/js")

// project(":mockk-client-tests-jvm").projectDir = file("client-tests/jvm")

// very weird hack to make it working in IDE and stay compatible with naming
if (gradle.startParameter.taskNames.contains("publish")) {
    project(":mockk-jvm").name = "mockk"
}
