plugins {
    buildsrc.convention.`kotlin-multiplatform`
    buildsrc.convention.`mockk-publishing`
}

description = "MockK BDD style aliases for DSL"

// BDD style API for MockK.
// Provides aliases for MockK functions in BDD style:
// - given/coGiven (aliases for every/coEvery)
// - then/coThen (aliases for verify/coVerify)

val mavenName: String by extra("MockK BDD")
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
