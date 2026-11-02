import buildsrc.config.androidClassesDexAttributes
import buildsrc.config.asConsumer

plugins {
    buildsrc.convention.`android-library`

    buildsrc.convention.`mockk-publishing`
}

description = "Android instrumented testing MockK inline mocking agent"

val mavenName: String by extra("MockK Android Agent")
val mavenDescription: String by extra("${project.description}")

val dispatcherJarResources = layout.buildDirectory.dir("generated/dispatcher-jar")

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
                srcDir(dispatcherJarResources)
            }
        }
    }

    defaultConfig {
        ndk {
            abiFilters += setOf("armeabi-v7a", "x86", "x86_64", "arm64-v8a")
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
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
    implementation(libs.dexmaker)
    implementation(libs.objenesis)

    androidClassesDex(projects.modules.mockkAgentAndroidDispatcher)

    testImplementation(kotlin("test-junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// The jar sits under the package directory, not at the root of the classpath: dexmaker-mockito-inline,
// which mockito-android depends on, ships a different root `dispatcher.jar`, and Android builds fail
// when two libraries contain a Java resource at the same path.
val packageDispatcherJar by tasks.registering(Jar::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    from(androidClassesDex.asFileTree)
    archiveFileName.set("dispatcher.jar")
    destinationDirectory.set(dispatcherJarResources.map { it.dir("io/mockk/proxy/android") })
}

tasks.preBuild {
    dependsOn(packageDispatcherJar)
}
