import buildsrc.config.Deps

plugins {
    buildsrc.convention.`kotlin-multiplatform`

    buildsrc.convention.`mockk-publishing`
}

description = "MockK DSL providing API for MockK implementation"

val mavenName: String by extra("MockK DSL")
val mavenDescription: String by extra("${project.description}")

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(dependencies.platform(Deps.Libs.kotlinCoroutinesBom))
                implementation(Deps.Libs.kotlinCoroutinesCore)
                implementation(kotlin("reflect"))
                implementation(projects.modules.mockkCore)
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
