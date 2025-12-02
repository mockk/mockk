import buildsrc.config.Deps
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration

plugins {
    buildsrc.convention.`kotlin-jvm-spring`
    buildsrc.convention.`mockk-publishing`
}

description = "MockBean and SpyBean, but for MockK instead of Mockito"

val mavenName: String by extra("MockK springmockk")
val mavenDescription: String by extra("${project.description}")

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["java"])
            }
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
        jvmArgs(
            "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
        )
    }
}

dependencies {
    api(projects.modules.mockk)
    api(Deps.Libs.spring7Test)
    api(Deps.Libs.spring7Context)
    implementation(Deps.Libs.kotlinReflect)

    testImplementation(Deps.Libs.springBoot4Test)
    testImplementation("jakarta.annotation:jakarta.annotation-api:3.0.0")

    testImplementation(Deps.Libs.junitJupiter6)
    testImplementation(Deps.Libs.junitJupiter6Params)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.1")
    testImplementation(Deps.Libs.assertj)
}
