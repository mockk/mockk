package buildsrc.convention

import buildsrc.config.createMockKPom
import buildsrc.config.credentialsAction


plugins {
    `maven-publish`
    signing
}

val sonatypeRepositoryCredentials: Provider<Action<PasswordCredentials>> =
    providers.credentialsAction("ossrh")

val sonatypeRepositoryReleaseUrl: Provider<String> = provider {
    if (version.toString().endsWith("SNAPSHOT")) {
        "https://oss.sonatype.org/content/repositories/snapshots/"
    } else {
        "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
    }
}

val signingKeyId: Provider<String> =
    providers.gradleProperty("signing.keyId")
val signingPassword: Provider<String> =
    providers.gradleProperty("signing.password")
val signingSecretKeyRingFile: Provider<String> =
    providers.gradleProperty("signing.secretKeyRingFile")
val ossrhUsername: Provider<String> =
    providers.gradleProperty("ossrhUsername")
val ossrhPassword: Provider<String> =
    providers.gradleProperty("ossrhPassword")


tasks.withType<AbstractPublishToMaven>().configureEach {
    // Gradle warns about some signing tasks using publishing task outputs without explicit
    // dependencies. Here's a quick fix.
    dependsOn(tasks.withType<Sign>())
    mustRunAfter(tasks.withType<Sign>())

    doLast {
        logger.lifecycle("[task: ${name}] ${publication.groupId}:${publication.artifactId}:${publication.version}")
    }
}

val mavenName: String by project.extra
val mavenDescription: String by project.extra
val localrepo: String by project

publishing {
    repositories {
        // publish to local dir, for testing
        maven(rootProject.layout.projectDirectory.dir(localrepo)) {
            name = "LocalRepo"
        }

        /*maven {
            url = uri(sonatypeRepositoryReleaseUrl)
            credentials {
                username = ossrhUsername.get()
                password = ossrhPassword.get()
            }
        }*/
    }
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

        if (signingKeyId.isPresent && signingSecretKeyRingFile.isPresent && signingPassword.isPresent) {
            signing.sign(this)
        }

    }
}

signing {
    if (signingKeyId.isPresent && signingSecretKeyRingFile.isPresent && signingPassword.isPresent) {
        logger.debug("[${project.displayName}] Signing is enabled")
    }
}

// workaround for https://github.com/gradle/gradle/issues/16543
inline fun <reified T : Task> TaskContainer.provider(taskName: String): Provider<T> =
    providers.provider { taskName }
        .flatMap { named<T>(it) }
