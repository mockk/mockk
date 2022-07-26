plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

group = "io.mockk"

tasks.wrapper {
    gradleVersion = "7.5"
    distributionType = Wrapper.DistributionType.ALL
}
