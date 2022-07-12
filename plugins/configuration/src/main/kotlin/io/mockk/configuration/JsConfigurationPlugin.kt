package io.mockk.configuration

import io.mockk.dependencies.Deps
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJsPlugin

class JsConfigurationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            apply<KotlinPlatformJsPlugin>()
            extensions.configure(KotlinJsProjectExtension::class) {
                sourceSets["main"].dependencies {
                    compileOnly(Deps.Libs.kotlinStdLibJs())
                }
                sourceSets["test"].dependencies {
                    implementation(Deps.Libs.kotlinTestJs())
                }
                js {
                    compilations.all {
                        kotlinOptions {
                            moduleKind = "commonjs"
                            sourceMap = true
                        }
                    }
                }
            }
        }
    }
}
