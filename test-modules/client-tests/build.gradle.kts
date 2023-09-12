import buildsrc.config.kotlinVersion

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
                implementation(enforcedPlatform(kotlin("bom", version = kotlinVersion())))
                implementation(kotlin("reflect"))

                implementation(platform(buildsrc.config.Deps.Libs.kotlinCoroutinesBom))
                implementation(buildsrc.config.Deps.Libs.kotlinCoroutinesCore)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(projects.modules.mockk)

                implementation(kotlin("test-junit5"))
            }
        }

        val jvmMain by getting {
            dependencies {
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(buildsrc.config.Deps.Libs.slfj)
                implementation(buildsrc.config.Deps.Libs.logback)

                implementation(buildsrc.config.Deps.Libs.junitJupiter)
            }
        }
    }
}

tasks.withType<Test> {
    // Forward the expected Kotlin version to unit tests
    environment("kotlin.version", kotlinVersion())
}
