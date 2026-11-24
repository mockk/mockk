plugins {
    buildsrc.convention.`kotlin-multiplatform`

    buildsrc.convention.`mockk-publishing`
}

description = "Mocking library for Kotlin"

val mavenName: String by extra("MockK")
val mavenDescription: String by extra("${project.description}")

tasks.withType<Test>().configureEach {
    filter {
        // Intentionally-failing test classes executed from
        // CheckUnnecessaryStubExtensionTest / CheckUnnecessaryStubRuleTest through the JUnit
        // Platform launcher (JUnit 4 variant through JUnitCore); they must not run directly,
        // otherwise their designed failure would break the suite.
        excludeTestsMatching("io.mockk.junit5.CheckUnnecessaryStubAttribution")
        excludeTestsMatching("io.mockk.junit4.CheckUnnecessaryStubAttribution")
    }

    doFirst {
        // Test runtime classpath for tests that fork a JVM themselves (StaticMockFilesTest, #1402).
        systemProperty("io.mockk.test.classpath", classpath.asPath)
    }
}

kotlin {
    jvm {
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.modules.mockkDsl)
                api(projects.modules.mockkAgent)
                api(projects.modules.mockkAgentApi)
                api(projects.modules.mockkCore)

                implementation(dependencies.platform(libs.kotlin.coroutines.bom))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

                implementation(kotlin("reflect"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
            }
        }
        val jvmMain by getting {
            dependencies {
                compileOnly(libs.slf4j)

                compileOnly(libs.junit4)
                compileOnly(dependencies.platform(libs.junit.bom))
                compileOnly("org.junit.jupiter:junit-jupiter")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.junit4)
                implementation(dependencies.platform(libs.junit.bom))
                implementation("org.junit.jupiter:junit-jupiter")
                implementation("org.junit.platform:junit-platform-testkit")
                implementation("org.junit.platform:junit-platform-launcher")
                // Reproduces issue #1220: relaxed mock of a kotlinx.datetime.Instant property
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            }
        }
    }
}
