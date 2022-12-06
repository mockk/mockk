import buildsrc.config.excludeGeneratedGradleDslAccessors

plugins {
    base
    org.jetbrains.kotlinx.`binary-compatibility-validator`
    org.jetbrains.kotlinx.kover
    idea

    // note: plugin versions are set in ./buildSrc/build.gradle.kts
}

group = "io.mockk"

koverMerged {
    enable()
}

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
        )
    }
}
