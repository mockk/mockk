plugins {
    kotlin("jvm") apply false
    id("buildsrc.convention.dokka")
}

dependencies {
    dokka(projects.modules.mockk)
    dokka(projects.modules.mockkAgent)
    dokka(projects.modules.mockkAgentAndroidDispatcher)
    dokka(projects.modules.mockkAgentApi)
    dokka(projects.modules.mockkAndroid)
    dokka(projects.modules.mockkBdd)
    dokka(projects.modules.mockkBddAndroid)
    dokka(projects.modules.mockkCore)
    dokka(projects.modules.mockkDsl)
}
