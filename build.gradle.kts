import buildsrc.config.excludeGeneratedGradleDslAccessors

plugins {
    base
    org.jetbrains.kotlinx.`binary-compatibility-validator`
    org.jetbrains.kotlinx.kover
    // note: plugin versions are set in ./buildSrc/build.gradle.kts
}

group = "io.mockk"

apiValidation {
    ignoredProjects += listOf(
        projects.testModules.loggerTests.name,
        projects.testModules.clientTests.name,
        projects.testModules.performanceTests.name,
    )
}

dependencies {
    kover(projects.modules.mockk)
    kover(projects.modules.mockkAgent)
    // if the android SDK is not available trying to resolve the module names will fail
    listOf("android", "agent-android").forEach { module ->
        rootProject.subprojects.find { it.name == "mockk-$module" }?.let {
            kover(it)
        }
    }

    kover(projects.testModules.loggerTests)
    kover(projects.testModules.clientTests)
    kover(projects.testModules.performanceTests)
}
