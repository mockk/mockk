package buildsrc.convention

import buildsrc.config.createMockKPom

val signingPassword: Provider<String> =
    providers.gradleProperty("signing.password")
val jreleaserPublicKey: Provider<String> =
    providers.gradleProperty("jreleaser.public.key")
val jreleaserPrivateKey: Provider<String> =
    providers.gradleProperty("jreleaser.private.key")
val ossrhUsername: Provider<String> =
    providers.gradleProperty("ossrhUsername")
val ossrhPassword: Provider<String> =
    providers.gradleProperty("ossrhPassword")

val mavenName: String by project.extra
val mavenDescription: String by project.extra

// workaround for https://github.com/gradle/gradle/issues/16543
inline fun <reified T : Task> TaskContainer.provider(taskName: String): Provider<T> =
    providers.provider { taskName }
        .flatMap { named<T>(it) }

plugins {
    `maven-publish`
    id("org.jreleaser")
}

publishing {
    // Configure for Android libraries
    publications {
        if (project.extensions.findByName("android") != null) {
            register<MavenPublication>("release") {
                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }
    publications.withType<MavenPublication>().configureEach {
        createMockKPom {
            name.set(provider { mavenName })
            description.set(provider { mavenDescription })
        }

        artifact(tasks.provider<Jar>("javadocJar"))

        repositories {
            maven {
                url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
            }
        }
    }
}

jreleaser {
    gitRootSearch = true
    deploy {
        maven {
            mavenCentral {
                active.set(org.jreleaser.model.Active.ALWAYS)
                register("sonatype") {
                    active = org.jreleaser.model.Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get().asFile.path)
                    username = ossrhUsername
                    password = ossrhPassword
                    maxRetries = 120
                    retryDelay = 60
                    verifyPom.set(false)
                }
            }
        }
    }
    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
        mode.set(org.jreleaser.model.Signing.Mode.FILE)
        passphrase.set(signingPassword)
        publicKey.set(jreleaserPublicKey)
        secretKey.set(jreleaserPrivateKey)
    }

    release {
        github {
            enabled = false
            skipRelease = true
            skipTag = true
        }
    }
}
