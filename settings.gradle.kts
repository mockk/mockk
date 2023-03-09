plugins {
  id("com.gradle.enterprise") version "3.12.3"
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
    publishAlways()
  }
}

rootProject.name = "mockk-root"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

apply(from = "./buildSrc/repositories.settings.gradle.kts")
apply(from = "./buildSrc/android-sdk-detector.settings.gradle.kts")

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
}

include(
    ":modules:mockk",
    ":modules:mockk-agent-api",
    ":modules:mockk-agent",
    ":modules:mockk-core",
    ":modules:mockk-dsl",

    ":test-modules:client-tests",
    ":test-modules:performance-tests",
    ":test-modules:logger-tests",
)

val androidSdkDetected: Boolean? by extra

if (androidSdkDetected == true) {
    include(
        ":modules:mockk-agent-android",
        ":modules:mockk-agent-android-dispatcher",
        ":modules:mockk-android",
    )
}
