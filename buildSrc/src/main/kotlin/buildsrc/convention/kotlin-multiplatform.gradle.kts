package buildsrc.convention

plugins {
    kotlin("multiplatform")

    id("org.jetbrains.dokka")

    id("buildsrc.convention.base")
}

kotlin {
    targets.all {
        compilations.all {
            kotlinOptions {
                apiVersion = "1.5"
                languageVersion = "1.7"
            }
        }
    }
}
