package buildsrc.convention

plugins {
    kotlin("multiplatform")

    id("org.jetbrains.dokka")

    id("buildsrc.convention.base")
}

kotlin {
    targets.configureEach {
        compilations.configureEach {
            kotlinOptions {
                apiVersion = "1.5"
                languageVersion = "1.7"
            }
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
}
