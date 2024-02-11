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
    implementation(Deps.Libs.kotlinReflect)

    implementation(Deps.Libs.springBootTest)
    implementation(Deps.Libs.springTest)
    implementation(Deps.Libs.springContext)

    testImplementation(Deps.Libs.junit4)
    testImplementation(Deps.Libs.junitJupiter)
    testImplementation(Deps.Libs.junitJupiterParams)
    testImplementation(Deps.Libs.assertj)
}
