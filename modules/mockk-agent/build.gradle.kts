import buildsrc.config.Deps

plugins {
    buildsrc.convention.`kotlin-multiplatform`

    buildsrc.convention.`mockk-publishing`
}

description = "MockK inline mocking agent"

val mavenName: String by extra("MockK")
val mavenDescription: String by extra("${project.description}")

kotlin {
    jvm {
        withJava()
    }

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
                api("org.objenesis:objenesis:${Deps.Versions.objenesis}")

                api("net.bytebuddy:byte-buddy:${Deps.Versions.byteBuddy}")
                api("net.bytebuddy:byte-buddy-agent:${Deps.Versions.byteBuddy}")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(Deps.Libs.junitJupiter)
            }
        }
    }
}
