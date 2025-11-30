plugins {
    buildsrc.convention.`kotlin-multiplatform`

    buildsrc.convention.`mockk-publishing`
}

description = "MockK inline mocking agent"

val mavenName: String by extra("MockK")
val mavenDescription: String by extra("${project.description}")

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.modules.mockkAgentApi)
                implementation(kotlin("reflect"))
                implementation(projects.modules.mockkCore)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(libs.objenesis)

                api(libs.byte.buddy.asProvider().get())
                api(libs.byte.buddy.agent)
            }
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
