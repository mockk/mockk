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

tasks.wrapper {
    gradleVersion = "7.5.1"
    distributionType = Wrapper.DistributionType.ALL
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
