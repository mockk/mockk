plugins {
    buildsrc.convention.`kotlin-multiplatform`
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(projects.modules.mockk)
            }
        }

        val jvmMain by getting {
            dependencies {
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(buildsrc.config.Deps.Libs.kotlinTestJunit()) {
                    exclude(group = "junit", module = "junit")
                }

                implementation(buildsrc.config.Deps.Libs.slfj)
                implementation(buildsrc.config.Deps.Libs.logback)

                implementation(buildsrc.config.Deps.Libs.junitJupiter)
            }
        }
    }
}
