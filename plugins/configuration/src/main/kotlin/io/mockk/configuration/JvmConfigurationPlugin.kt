package io.mockk.configuration

import io.mockk.dependencies.Deps
import io.mockk.dependencies.kotlinVersion
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin

class JvmConfigurationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            configureKotlinJvm()
            configureJavaPlugin()
            configureDokka()
        }
    }

    private fun Project.configureKotlinJvm() {
        apply<KotlinPlatformJvmPlugin>()
        extensions.configure(KotlinJvmProjectExtension::class) {
            sourceSets["main"].dependencies {
                implementation(Deps.Libs.kotlinStdLib(kotlinVersion()))
                compileOnly(Deps.Libs.junitJupiterApi)
            }
            sourceSets["test"].dependencies {
                implementation(Deps.Libs.kotlinTestJunit(kotlinVersion())) {
                    exclude(group = "junit", module = "junit")
                }
                implementation(Deps.Libs.slfj)
                implementation(Deps.Libs.logback)
                implementation(Deps.Libs.junitJupiterApi)
                implementation(Deps.Libs.junitJupiterEngine)
                implementation(Deps.Libs.junitVintageEngine)
            }
        }
    }

    private fun Project.configureJavaPlugin() {
        extensions.configure(JavaPluginExtension::class) {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        tasks {
            named<Test>("test") {
                testLogging {
                    exceptionFormat = TestExceptionFormat.FULL
                }
                useJUnitPlatform()
            }
        }
    }

    private fun Project.configureDokka() {
        apply<DokkaPlugin>()
        tasks {
            val dokkaJavadoc = named<DokkaTask>("dokkaJavadoc") {
                outputDirectory.set(file("$buildDir/javadoc"))
            }
            register<Jar>("javadocJar") {
                dependsOn(dokkaJavadoc)
                archiveClassifier.set("javadoc")
                from("$buildDir/javadoc")
            }
        }
    }
}
