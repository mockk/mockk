plugins {
    id("mpp-jvm")
}

extra["mavenName"] = "MockK Java Agent API"
extra["mavenDescription"] = "API to build MockK agents"

apply(from = "${rootProject.extensions.extraProperties["gradles"]}/jacoco.gradle")
apply(from = "${rootProject.extensions.extraProperties["gradles"]}/additional-archives.gradle")
apply(from = "${rootProject.extensions.extraProperties["gradles"]}/upload.gradle")
