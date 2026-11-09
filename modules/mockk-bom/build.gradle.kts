plugins {
    `java-platform`

    buildsrc.convention.`mockk-publishing`
}

description = "MockK Bill of Materials (BOM) with managed versions for all MockK artifacts"

val mavenName: String by extra("MockK BOM")
val mavenDescription: String by extra("${project.description}")

group = rootProject.group
version = rootProject.version

val mockkVersion = rootProject.version.toString()

dependencies {
    constraints {
        // core artifacts
        api("io.mockk:mockk:$mockkVersion")
        api("io.mockk:mockk-agent:$mockkVersion")
        api("io.mockk:mockk-agent-api:$mockkVersion")
        api("io.mockk:mockk-core:$mockkVersion")
        api("io.mockk:mockk-dsl:$mockkVersion")
        api("io.mockk:mockk-bdd:$mockkVersion")
        api("io.mockk:mockk-context-parameters:$mockkVersion")

        // JVM variants published by the Kotlin multiplatform modules
        api("io.mockk:mockk-jvm:$mockkVersion")
        api("io.mockk:mockk-agent-jvm:$mockkVersion")
        api("io.mockk:mockk-agent-api-jvm:$mockkVersion")
        api("io.mockk:mockk-core-jvm:$mockkVersion")
        api("io.mockk:mockk-dsl-jvm:$mockkVersion")
        api("io.mockk:mockk-bdd-jvm:$mockkVersion")
        api("io.mockk:mockk-context-parameters-jvm:$mockkVersion")

        // Android artifacts
        api("io.mockk:mockk-android:$mockkVersion")
        api("io.mockk:mockk-agent-android:$mockkVersion")
        api("io.mockk:mockk-bdd-android:$mockkVersion")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["javaPlatform"])
        }
    }
}
