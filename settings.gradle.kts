plugins {
    id("com.gradle.develocity").version("4.2.2")
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}

rootProject.name = "mockk-root"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

apply(from = "./buildSrc/repositories.settings.gradle.kts")
apply(from = "./buildSrc/android-sdk-detector.settings.gradle.kts")

include(
    ":modules:mockk",
    ":modules:mockk-agent-api",
    ":modules:mockk-agent",
    ":modules:mockk-core",
    ":modules:mockk-dsl",
    ":modules:mockk-bdd",
    ":modules:docs",

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
        ":modules:mockk-bdd-android",
    )
}
