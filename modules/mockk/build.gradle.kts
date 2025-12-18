plugins {
    buildsrc.convention.`kotlin-multiplatform`

    buildsrc.convention.`mockk-publishing`
}

description = "Mocking library for Kotlin"

val mavenName: String by extra("MockK")
val mavenDescription: String by extra("${project.description}")

kotlin {
    jvm {
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.modules.mockkDsl)
                api(projects.modules.mockkAgent)
                api(projects.modules.mockkAgentApi)
                api(projects.modules.mockkCore)

                implementation(dependencies.platform(libs.kotlin.coroutines.bom))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

                implementation(kotlin("reflect"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
            }
        }
        val jvmMain by getting {
            dependencies {
                compileOnly(libs.slf4j)

                compileOnly(libs.junit4)
                compileOnly(dependencies.platform(libs.junit.bom))
                compileOnly("org.junit.jupiter:junit-jupiter")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.junit4)
                implementation(dependencies.platform(libs.junit.bom))
                implementation("org.junit.jupiter:junit-jupiter")
                runtimeOnly("org.junit.platform:junit-platform-launcher")
            }
        }
    }
}
