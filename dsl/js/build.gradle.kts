import io.mockk.dependencies.Deps

plugins {
    id("mpp-js")
}

extra["mavenName"] = "JS MockK DSL"
extra["mavenDescription"] = "JS MockK DSL providing API for MockK implementation"

apply from: "${rootProject.extensions.extraProperties["gradles"]}/additional-archives.gradle"
apply from: "${rootProject.extensions.extraProperties["gradles"]}/upload.gradle"

dependencies {
    expectedBy(project(":mockk-dsl"))
    compileOnly(Deps.Libs.kotlinCoroutinesCoreJs())
}

tasks {
    val sourcesJar by creating(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
        from(project(":mockk-dsl").sourceSets["main"].allJava.files) {
            exclude("io/mockk/InternalPlatformDsl.kt")
            exclude("io/mockk/MockKSettings.kt")
        }
    }
    artifacts {
        add("archives", sourcesJar)
    }
}
