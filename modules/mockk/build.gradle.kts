plugins {
    buildsrc.convention.`kotlin-multiplatform`
}

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.modules.mockkDsl)
                implementation(projects.modules.mockkAgent)
                implementation(projects.modules.mockkAgentApi)

                implementation(buildsrc.config.Deps.Libs.kotlinCoroutinesCore())

                implementation(kotlin("reflect"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(buildsrc.config.Deps.Libs.slfj)

                implementation(buildsrc.config.Deps.Libs.junit4)
                implementation(buildsrc.config.Deps.Libs.junitJupiter)
            }
        }
        val jvmTest by getting {
            dependencies {
            }
        }
    }
}
