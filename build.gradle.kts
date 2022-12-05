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
        projects.testModules.performanceTests.name,
        projects.testModules.clientTests.name,
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
