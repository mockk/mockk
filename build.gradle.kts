plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    idea
}

group = "io.mockk"

tasks.wrapper {
    gradleVersion = "7.5"
    distributionType = Wrapper.DistributionType.ALL
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
