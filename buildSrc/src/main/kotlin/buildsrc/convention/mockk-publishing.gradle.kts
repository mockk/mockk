package buildsrc.convention

import buildsrc.config.createMockKPom
import buildsrc.config.credentialsAction
import org.gradle.api.tasks.bundling.Jar


plugins {
    `maven-publish`
    signing
}

val sonatypeRepositoryCredentials: Provider<Action<PasswordCredentials>> =
    providers.credentialsAction("ossrh")

val sonatypeRepositoryReleaseUrl: Provider<String> = provider {
    if (version.toString().endsWith("SNAPSHOT")) {
        "https://s01.oss.sonatype.org/content/repositories/snapshots/"
    } else {
        "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
    }
}

val signingKeyId: Provider<String> =
    providers.gradleProperty("signing.keyId")
val signingKey: Provider<String> =
    providers.gradleProperty("signing.key")
val signingPassword: Provider<String> =
    providers.gradleProperty("signing.password")
val signingSecretKeyRingFile: Provider<String> =
    providers.gradleProperty("signing.secretKeyRingFile")


tasks.withType<AbstractPublishToMaven>().configureEach {
    // Gradle warns about some signing tasks using publishing task outputs without explicit
    // dependencies. Here's a quick fix.
    dependsOn(tasks.withType<Sign>())
    mustRunAfter(tasks.withType<Sign>())

    doLast {
        logger.lifecycle("[${this.name}] ${project.group}:${project.name}:${project.version}")
    }
}

publishing {
    repositories {
        // publish to local dir, for testing
        maven(rootProject.layout.buildDirectory.dir("maven-internal")) {
            name = "LocalProjectDir"
        }
    }
    publications.withType<MavenPublication>().configureEach {
        createMockKPom()

        artifact(tasks.provider<Jar>("javadocJar"))

        signing.sign(this)
    }
}

signing {
    if (signingKeyId.isPresent() && signingKey.isPresent() && signingPassword.isPresent()) {
        logger.lifecycle("[${project.displayName}] Signing is enabled")
        useInMemoryPgpKeys(signingKeyId.get(), signingKey.get(), signingPassword.get())
    }
}

// workaround for https://github.com/gradle/gradle/issues/16543
inline fun <reified T : Task> TaskContainer.provider(taskName: String): Provider<T> =
    providers.provider { taskName }
        .flatMap { named<T>(it) }
