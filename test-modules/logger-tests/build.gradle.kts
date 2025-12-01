plugins {
    buildsrc.convention.`kotlin-multiplatform`
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(dependencies.platform(kotlin("bom")))
                implementation(kotlin("reflect"))

                implementation(dependencies.platform(libs.kotlin.coroutines.bom))
                implementation(libs.kotlin.coroutines.core)
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
                implementation(dependencies.platform(libs.junit.bom))
                implementation("org.junit.jupiter:junit-jupiter")
                runtimeOnly("org.junit.platform:junit-platform-launcher")
            }
        }
    }
}
