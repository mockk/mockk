package io.mockk.configuration

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import io.mockk.dependencies.Deps
import io.mockk.dependencies.kotlinVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper

class AndroidConfigurationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            configureAndroid()
            configureDokka()
        }
    }

    private fun Project.configureAndroid() {
        apply<LibraryPlugin>()
        apply<KotlinAndroidPluginWrapper>()
        extensions.configure(KotlinAndroidProjectExtension::class) {
            sourceSets["main"].dependencies {
                implementation(Deps.Libs.kotlinStdLib(kotlinVersion()))
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
