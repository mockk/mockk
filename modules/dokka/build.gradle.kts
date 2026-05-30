plugins {
    kotlin("jvm") apply false
    id("buildsrc.convention.dokka")
}

dependencies {
    dokka(projects.modules.mockk)
    dokka(projects.modules.mockkAgent)
    listOf(
        ":modules:mockk-agent-android",
        ":modules:mockk-agent-android-dispatcher",
        ":modules:mockk-android",
        ":modules:mockk-bdd-android",
    ).forEach { path -> findProject(path)?.let { dokka(it) } }
    dokka(projects.modules.mockkAgentApi)
    dokka(projects.modules.mockkBdd)
    dokka(projects.modules.mockkCore)
    dokka(projects.modules.mockkDsl)
}

dokka {
    moduleName.set("MockK API")

    dokkaPublications.html {
        includes.from("MockK.md")
    }
}
