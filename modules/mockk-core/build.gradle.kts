import buildsrc.config.Deps

plugins {
    buildsrc.convention.`kotlin-multiplatform`

    buildsrc.convention.`mockk-publishing`
}

description = "MockK functionality that is used by other MockK modules"

val mavenName: String by extra("MockK Core")
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
                implementation(kotlin("reflect"))
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
