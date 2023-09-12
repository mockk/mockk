import buildsrc.config.excludeGeneratedGradleDslAccessors

plugins {
    base
    org.jetbrains.kotlinx.`binary-compatibility-validator`
    org.jetbrains.kotlinx.kover
    idea

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

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true

        excludeGeneratedGradleDslAccessors(layout)
        excludeDirs = excludeDirs + layout.files(
            ".idea",
            "gradle/wrapper",
            "modules/mockk-agent-android/.cxx",
        )
    }
}

dependencies {
    kover(projects.modules.mockk)
    kover(projects.modules.mockkAgent)
    kover(projects.modules.mockkAndroid)
    kover(projects.modules.mockkAgentAndroid)
    kover(projects.testModules.loggerTests)
    kover(projects.testModules.clientTests)
    kover(projects.testModules.performanceTests)
}
