plugins {
    kotlin("jvm") apply false
    id("buildsrc.convention.dokka")
}

dependencies {
    dokka(projects.modules.mockk)
    dokka(projects.modules.mockkAgent)
    dokka(projects.modules.mockkAgentApi)
    dokka(projects.modules.mockkBdd)
    dokka(projects.modules.mockkCore)
    dokka(projects.modules.mockkDsl)

    // Android modules are only part of the build when an Android SDK is found,
    // see buildSrc/android-sdk-detector.settings.gradle.kts
    listOf(
        ":modules:mockk-agent-android",
        ":modules:mockk-agent-android-dispatcher",
        ":modules:mockk-android",
        ":modules:mockk-bdd-android",
    ).mapNotNull(::findProject).forEach { dokka(it) }
}

dokka {
    moduleName.set("MockK API")

    dokkaPublications.html {
        includes.from("MockK.md")
    }
}
