import buildsrc.config.androidClassesDexAttributes
import buildsrc.config.asConsumer

plugins {
    buildsrc.convention.`android-library`

    buildsrc.convention.`mockk-publishing`
}

description = "Android instrumented testing MockK inline mocking agent"

val mavenName: String by extra("MockK Android Agent")
val mavenDescription: String by extra("${project.description}")

@Suppress("UnstableApiUsage")
android {
    namespace = "io.mockk.proxy.android"
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
        }
    }

    sourceSets {
        named("main").configure {
            resources {
                srcDirs({ packageDispatcherJar.map { it.destinationDirectory } })
            }
        }
    }

    defaultConfig {
        ndk {
            abiFilters += setOf("armeabi-v7a", "x86", "x86_64", "arm64-v8a")
        }
    }
}

val androidClassesDex: Configuration by configurations.creating {
    description = "Fetch Android classes.dex files"
    asConsumer()
    androidClassesDexAttributes()
}

dependencies {
    api(projects.modules.mockkAgentApi)
    api(projects.modules.mockkAgent)

    implementation(projects.modules.mockkCore)

    implementation(kotlin("reflect"))
    implementation("com.linkedin.dexmaker:dexmaker:${buildsrc.config.Deps.Versions.dexmaker}")
    implementation("org.objenesis:objenesis:${buildsrc.config.Deps.Versions.objenesis}")

    androidClassesDex(projects.modules.mockkAgentAndroidDispatcher)
}

val packageDispatcherJar by tasks.registering(Jar::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    from(androidClassesDex.asFileTree)
    archiveFileName.set("dispatcher.jar")
    destinationDirectory.set(temporaryDir)
}

tasks.preBuild {
    dependsOn(packageDispatcherJar)
}
