package buildsrc.config

import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication


fun MavenPublication.createMockKPom(
    configure: MavenPom.() -> Unit = {}
): Unit =
    pom {
        url.set("https://mockk.io")
        scm {
            connection.set("scm:git:git@github.com:mockk/mockk.git")
            developerConnection.set("scm:git:git@github.com:mockk/mockk.git")
            url.set("https://github.com/mockk/mockk/")
        }

        developers {
            developer {
                id.set("oleksiyp")
                name.set("Oleksii Pylypenko")
                email.set("oleksiy.pylypenko@gmail.com")
            }
            developer {
                id.set("Raibaz")
                name.set("Mattia Tommasone")
                email.set("raibaz@gmail.com")
            }
        }

        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        configure()
    }

/**
 * Fetches credentials from `gradle.properties`, environment variables, or command line args.
 *
 * See https://docs.gradle.org/current/userguide/declaring_repositories.html#sec:handling_credentials
 */
// https://github.com/gradle/gradle/issues/20925
fun ProviderFactory.credentialsAction(
    repositoryName: String
): Provider<Action<PasswordCredentials>> = zip(
    gradleProperty("${repositoryName}Username"),
    gradleProperty("${repositoryName}Password"),
) { user, pass ->
    Action<PasswordCredentials> {
        username = user
        password = pass
    }
}
