plugins {
    id("mpp-common")
}

extra["mavenName"] = "MockK common"
extra["mavenDescription"] = "Common(JS and Java) MockK module"

apply(from = "${rootProject.extensions.extraProperties["gradles"]}/additional-archives.gradle")
apply(from = "${rootProject.extensions.extraProperties["gradles"]}/upload.gradle")

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    api(project(":mockk-dsl"))
}
