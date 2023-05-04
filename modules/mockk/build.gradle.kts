import buildsrc.config.Deps

plugins {
    buildsrc.convention.`kotlin-multiplatform`

    buildsrc.convention.`mockk-publishing`
}

description = "Mocking library for Kotlin"

val mavenName: String by extra("MockK")
val mavenDescription: String by extra("${project.description}")

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.modules.mockkDsl)
                api(projects.modules.mockkAgent)
                api(projects.modules.mockkAgentApi)
                api(projects.modules.mockkCore)

                implementation(dependencies.platform(Deps.Libs.kotlinCoroutinesBom))
                implementation(Deps.Libs.kotlinCoroutinesCore)

                implementation(kotlin("reflect"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(Deps.Libs.kotlinCoroutinesTest)
            }
        }
        val jvmMain by getting {
            dependencies {
                compileOnly(Deps.Libs.slfj)

                implementation(Deps.Libs.junit4)
                implementation(Deps.Libs.junitJupiter)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(Deps.Libs.junitJupiter)
            }
        }
    }
}
