plugins {
    id("mpp-jvm")
}

extra["mavenName"] = "MockK Common Agent classes"
extra["mavenDescription"] = "Common classes for agents"

apply(from = "${rootProject.extensions.extraProperties["gradles"]}/jacoco.gradle")
apply(from = "${rootProject.extensions.extraProperties["gradles"]}/additional-archives.gradle")
apply(from = "${rootProject.extensions.extraProperties["gradles"]}/upload.gradle")

dependencies {
    api(project(":mockk-agent-api"))
}