package buildsrc.convention

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    dokkaSourceSets.configureEach {
        sourceLink {
            // Read docs for more details: https://kotlinlang.org/docs/dokka-gradle.html#source-link-configuration
            remoteUrl("https://github.com/mockk/mockk/tree/master")
            localDirectory.set(rootDir)
        }
    }
}
