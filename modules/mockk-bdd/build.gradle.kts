plugins {
    buildsrc.convention.`kotlin-multiplatform`
    buildsrc.convention.`mockk-publishing`
}

description = "MockK BDD style aliases for DSL"

val mavenName: String by extra("MockK BDD")
val mavenDescription: String by extra("${project.description}")

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(dependencies.platform(libs.kotlin.coroutines.bom))
                implementation(libs.kotlin.coroutines.core)
                implementation(kotlin("reflect"))
                implementation(projects.modules.mockkCore)
                implementation(projects.modules.mockkDsl)
                implementation(projects.modules.mockk)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit.jupiter)
            }
        }
    }
} 
