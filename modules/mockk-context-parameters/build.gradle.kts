plugins {
    buildsrc.convention.`kotlin-multiplatform`
    buildsrc.convention.`mockk-publishing`
}

description = "MockK DSL extensions for Kotlin context parameters"

// Provides `withContext` helper functions for stubbing and verifying
// functions that use Kotlin context parameters.
// This is a separate module to avoid breaking compatibility with Kotlin < 2.3.

val mavenName: String by extra("MockK Context Parameters")
val mavenDescription: String by extra("${project.description}")

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(dependencies.platform(libs.kotlin.coroutines.bom))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
                implementation(kotlin("reflect"))
                implementation(projects.modules.mockkCore)
                implementation(projects.modules.mockkDsl)
                implementation(projects.modules.mockk)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(dependencies.platform(libs.junit.bom))
                implementation("org.junit.jupiter:junit-jupiter")
                runtimeOnly("org.junit.platform:junit-platform-launcher")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}
