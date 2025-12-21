package buildsrc.convention

plugins {
    id("com.diffplug.spotless")
}

spotless {
    kotlin {
        ktlint()
        target("src/*/kotlin/**/*.kt", "src/*/java/**/*.kt")
        // remove exclusion, when JS functionality is added
        targetExclude("src/jsMain/kotlin/io/mockk/impl/JsMockKGateway.kt")
    }
    kotlinGradle {
        ktlint()
    }
}
