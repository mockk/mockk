import buildsrc.config.Deps

plugins {
    buildsrc.convention.`kotlin-multiplatform`

    buildsrc.convention.`mockk-publishing`
}

description = "MockK inline mocking agent"

val mavenName: String by extra("MockK")
val mavenDescription: String by extra("${project.description}")

val byteBuddyVersion = Deps.Versions.byteBuddy
val objenesisVersion = Deps.Versions.objenesis

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
                implementation(kotlin("test-junit5"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api("org.objenesis:objenesis:$objenesisVersion")

                api("net.bytebuddy:byte-buddy:$byteBuddyVersion")
                api("net.bytebuddy:byte-buddy-agent:$byteBuddyVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(Deps.Libs.junitJupiter)
            }
        }
    }
}
