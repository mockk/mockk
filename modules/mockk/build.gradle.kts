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
                implementation(projects.modules.mockkDsl)
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
                implementation("org.slf4j:slf4j-api:1.7.36")
                implementation("junit:junit:4.13.2")

                implementation(buildsrc.config.Deps.Libs.junitJupiter)
            }
        }
        val jvmTest by getting {
            dependencies {
            }
        }
    }
}
