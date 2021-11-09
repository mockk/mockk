plugins {
    id("mpp-jvm")
}

extra["mavenName"] = "MockK Java Agent"
extra["mavenDescription"] = "MockK inline mocking agent"

apply(from = "${rootProject.extensions.extraProperties["gradles"]}/jacoco.gradle")
apply(from = "${rootProject.extensions.extraProperties["gradles"]}/additional-archives.gradle")
apply(from = "${rootProject.extensions.extraProperties["gradles"]}/upload.gradle")

dependencies {
    api(project(':mockk-agent-api'))
    api(project(':mockk-agent-common'))

    api(Deps.Libs.objenesis)
    api(Deps.Libs.bytebuddy)
    api(Deps.Libs.bytebuddyAgent)

    implementation(Deps.Plugins.kotlinReflect(kotlinVersion()))
}

val copyMockKDispatcher = tasks.register<Copy>("copyMockKDispatcher") {
    dependsOn(tasks.compileJava)
    dependsOn(tasks.compileKotlin)
    from("${sourceSets.main.java.outputDir}/io/mockk/proxy/jvm/dispatcher")
    include("JvmMockKDispatcher.class")
    include("JvmMockKWeakMap.class")
    include("JvmMockKWeakMap\$StrongKey.class")
    include("JvmMockKWeakMap\$WeakKey.class")
    into("${sourceSets.main.java.outputDir}/io/mockk/proxy/jvm/dispatcher")
    rename {
        it.replace(".class", ".clazz")
    }
}

tasks.named("classes").configure {
    dependsOn(copyMockKDispatcher)}
}

tasks.jar {
    exclude("io/mockk/proxy/jvm/dispatcher/JvmMockKDispatcher.class")
    exclude("io/mockk/proxy/jvm/dispatcher/JvmMockKWeakMap*.class")
}
