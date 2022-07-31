plugins {
    buildsrc.convention.`kotlin-multiplatform`

    buildsrc.convention.`mockk-publishing`
}

description = "MockK inline mocking agent"

val mavenName: String by extra("MockK")
val mavenDescription: String by extra("${project.description}")

val byteBuddyVersion = "1.12.10"
val objenesisVersion = "3.2"

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.modules.mockkAgentApi)
                implementation(kotlin("reflect"))
            }
        }
        val commonTest by getting {
            dependencies {
            }
        }
        val jvmMain by getting {
            dependencies {
//                api (project(":mockk-agent-common"))

                api("org.objenesis:objenesis:$objenesisVersion")

                api("net.bytebuddy:byte-buddy:$byteBuddyVersion")
                api("net.bytebuddy:byte-buddy-agent:$byteBuddyVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(buildsrc.config.Deps.Libs.junitJupiter)
                implementation(buildsrc.config.Deps.Libs.junitVintageEngine)
            }
        }
    }
}
