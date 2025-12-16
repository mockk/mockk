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
            }
        }

        val jvmMain by getting {
            dependencies {
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(Deps.Libs.slfj)
                implementation(Deps.Libs.logback)

                implementation(dependencies.platform(Deps.Libs.junitBom))
                implementation("org.junit.jupiter:junit-jupiter")
                runtimeOnly("org.junit.platform:junit-platform-launcher")
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
