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

    listOf(
        ":modules:mockk-agent-android",
        ":modules:mockk-agent-android-dispatcher",
        ":modules:mockk-android",
        ":modules:mockk-bdd-android",
    ).forEach { path ->
        rootProject.findProject(path)?.let { dokka(it) }
    }
}

dokka {
    moduleName.set("MockK API")

    dokkaPublications.html {
        includes.from("MockK.md")
    }
}
