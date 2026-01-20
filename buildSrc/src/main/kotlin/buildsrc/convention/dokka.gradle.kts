package buildsrc.convention

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    pluginsConfiguration.html {
        homepageLink = "https://mockk.io"
        footerMessage = "© 2026 MockK Authors"
    }

    dokkaSourceSets.configureEach {
        sourceLink {
            remoteUrl("https://github.com/mockk/mockk/tree/master")
            localDirectory.set(rootDir)
        }
    }
}
