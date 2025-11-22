plugins {
    buildsrc.convention.`kotlin-multiplatform`
    jacoco
}
val kotlinVersion = providers.gradleProperty("io_mockk_kotlin_version").orNull ?: libs.versions.kotlin.get()

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project.dependencies.enforcedPlatform(kotlin("bom", version = kotlinVersion)))
                implementation(kotlin("reflect"))

                implementation(project.dependencies.platform(libs.kotlin.coroutines.bom))
                implementation(libs.kotlin.coroutines.core)
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
                implementation(libs.slf4j)
                implementation(libs.logback)

                implementation(libs.junit.jupiter)
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
    environment("kotlin.version", kotlinVersion)
    useJUnitPlatform()
    finalizedBy(tasks.getByName("jacocoTestReport"))
}
