import buildsrc.config.Deps
import buildsrc.config.kotlinVersion

plugins {
    buildsrc.convention.`kotlin-multiplatform`
    jacoco
}

kotlin {
    jvm {
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project.dependencies.enforcedPlatform(kotlin("bom", version = kotlinVersion())))
                implementation(kotlin("reflect"))

                implementation(project.dependencies.platform(Deps.Libs.kotlinCoroutinesBom))
                implementation(Deps.Libs.kotlinCoroutinesCore)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(projects.modules.mockk)

                implementation(kotlin("test-junit5"))

                implementation("io.arrow-kt:arrow-core:2.1.2")
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

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.withType<Test>())

    reports {
        html.required.set(true)
        xml.required.set(false)
        csv.required.set(false)
    }
}

tasks.withType<Test> {
    // Forward the expected Kotlin version to unit tests
    environment("kotlin.version", kotlinVersion())
    useJUnitPlatform()
    finalizedBy(tasks.getByName("jacocoTestReport"))
}
