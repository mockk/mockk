plugins {
    buildsrc.convention.`kotlin-multiplatform`
    jacoco
}

val kotlinVersion: String =
    providers
        .gradleProperty("io_mockk_kotlin_version")
        .getOrElse(libs.versions.kotlin.get())

val coroutinesVersion: String =
    if (providers.gradleProperty("io_mockk_kotlin_version").isPresent) {
        when {
            kotlinVersion.startsWith("1.5") -> "1.5.2"
            kotlinVersion.startsWith("1.6") -> "1.6.4"
            kotlinVersion.startsWith("1.7") -> "1.6.4"
            kotlinVersion.startsWith("1.8") -> "1.7.3"
            kotlinVersion.startsWith("1.9") -> "1.8.1"
            else -> libs.versions.coroutines.get()
        }
    } else {
        libs.versions.coroutines.get()
    }

kotlin {
    jvm {}

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(dependencies.enforcedPlatform(kotlin("bom", version = kotlinVersion)))
                implementation(kotlin("reflect"))

                implementation(dependencies.enforcedPlatform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
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

                implementation(dependencies.platform(libs.junit.bom))
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
    environment("kotlin.version", kotlinVersion)
    useJUnitPlatform()
    finalizedBy(tasks.getByName("jacocoTestReport"))
}
