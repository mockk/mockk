import io.mockk.dependencies.Deps
import io.mockk.dependencies.kotlinVersion

plugins {
    id("mpp-jvm")
}

extra["mavenName"] = "MockK"
extra["mavenDescription"] = "mocking library for Kotlin"

apply(from = "${rootProject.extensions.extraProperties["gradles"]}/jacoco.gradle")
apply(from = "${rootProject.extensions.extraProperties["gradles"]}/additional-archives.gradle")
apply(from = "${rootProject.extensions.extraProperties["gradles"]}/upload.gradle")

dependencies {
    expectedBy(project(":mockk-common"))
    api(project(":mockk-dsl-jvm"))
    implementation(project(":mockk-agent-jvm"))

    implementation(Deps.Libs.kotlinReflect(kotlinVersion()))
    compileOnly(Deps.Libs.kotlinCoroutinesCore())
    compileOnly("org.slf4j:slf4j-api:1.7.36")
    compileOnly("junit:junit:4.13.2")

    testImplementation(Deps.Libs.kotlinCoroutinesCore())
}

evaluationDependsOn(":mockk-common")
tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
        /*from(project(":mockk-dsl").sourceSets["main"].allJava.files) {
            exclude("io/mockk/impl/InternalPlatform.kt")
            exclude("io/mockk/impl/annotations/AdditionalInterface.kt")
            exclude("io/mockk/MockK.kt")
        }*/
    }
}

base {
    archivesBaseName = "mockk"
}
