plugins {
    id("mpp-jvm")
}

extra["mavenName"] = "Java MockK DSL"
extra["mavenDescription"] = "Java MockK DSL providing API for MockK implementation"

apply(from = "${rootProject.extensions.extraProperties["gradles"]}/jacoco.gradle")
apply(from = "${rootProject.extensions.extraProperties["gradles"]}/additional-archives.gradle")
apply(from = "${rootProject.extensions.extraProperties["gradles"]}/upload.gradle")

dependencies {
    expectedBy(project(":mockk-dsl"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:_")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:_")
}

evaluationDependsOn(":mockk-dsl")

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
