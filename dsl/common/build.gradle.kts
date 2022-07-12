plugins {
    id("mpp-common")
}

extra["mavenName"] = "MockK DSL common"
extra["mavenDescription"] = "Common(JS and Java) MockK DSL providing API for MockK implementation"

apply(from = "${rootProject.extensions.extraProperties["gradles"]}/additional-archives.gradle")
apply(from = "${rootProject.extensions.extraProperties["gradles"]}/upload.gradle")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
