import buildsrc.config.Deps

plugins {
    buildsrc.convention.`kotlin-multiplatform`
}

kotlin {
    jvm {
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(dependencies.platform(kotlin("bom")))
                implementation(kotlin("reflect"))

                implementation(dependencies.platform(buildsrc.config.Deps.Libs.kotlinCoroutinesBom))
                implementation(buildsrc.config.Deps.Libs.kotlinCoroutinesCore)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(projects.modules.mockk) {
                    exclude("org.slf4j", "slf4j-api")
                }

                implementation(kotlin("test-junit5"))
            }
        }

        val jvmMain by getting {
            dependencies {}
        }

        val jvmTest by getting {
            dependencies {
                implementation(dependencies.platform(Deps.Libs.junitBom))
                implementation("org.junit.jupiter:junit-jupiter")
                runtimeOnly("org.junit.platform:junit-platform-launcher")
            }
        }
    }
}
