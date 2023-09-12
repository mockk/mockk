import buildsrc.config.Deps

plugins {
    buildsrc.convention.`kotlin-multiplatform`

    buildsrc.convention.`mockk-publishing`
}

description = "API to build MockK agents"

val mavenName: String by extra("MockK Agent API")
val mavenDescription: String by extra("${project.description}")

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
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
                implementation(Deps.Libs.junitJupiter)
            }
        }
    }
}
