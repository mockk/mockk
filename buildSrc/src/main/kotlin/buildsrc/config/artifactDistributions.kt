package buildsrc.config

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute


/**
 * Marks this [configuration][Configuration] as a **provider** of artifacts.
 *
 * See https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:resolvable-consumable-configs
 */
fun Configuration.asProvider() {
    isVisible = false
    isCanBeResolved = false
    isCanBeConsumed = true
}


/**
 * Marks this [configuration][Configuration] as a **consumer** of artifacts.
 *
 * See https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:resolvable-consumable-configs
 */
fun Configuration.asConsumer() {
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
}


private val mockkResourceAttribute = Attribute.of("io.mockk.resource", String::class.java)


/**
 * Adds `android-classes-dex` attributes to this [configuration][Configuration].
 *
 * This allows for 'variant-aware' sharing of artifacts between projects.
 *
 * See https://docs.gradle.org/current/userguide/cross_project_publications.html#sec:variant-aware-sharing
 */
fun Configuration.androidClassesDexAttributes(): Configuration =
    attributes {
        attribute(mockkResourceAttribute, "android-classes-dex")
    }
