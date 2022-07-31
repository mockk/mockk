import buildsrc.config.Deps

plugins {
    buildsrc.convention.`kotlin-multiplatform`

    buildsrc.convention.`mockk-publishing`
}

description = "MockK DSL providing API for MockK implementation"

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Deps.Libs.kotlinCoroutinesCore())
                implementation(kotlin("reflect"))
            }
        }
        val commonTest by getting {
            dependencies {
            }
        }
        val jvmMain by getting {
            dependencies {
            }
        }
        val jvmTest by getting {
            dependencies {
            }
        }
    }
}
