package io.mockk.configuration

import io.mockk.dependencies.Deps
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformCommonPlugin

class CommonConfigurationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            apply<KotlinPlatformCommonPlugin>()
            extensions.configure(KotlinCommonProjectExtension::class) {
                sourceSets["main"].dependencies {
                    implementation(Deps.Libs.kotlinStdLib())
                    implementation(Deps.Libs.kotlinReflect())
                }
                sourceSets["test"].dependencies {
                    implementation(Deps.Libs.kotlinTestCommon())
                    implementation(Deps.Libs.kotlinTestCommonAnnotations())
                }
            }
        }
    }
}
