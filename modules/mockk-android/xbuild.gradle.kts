import buildsrc.config.Deps

plugins {
    buildsrc.convention.`kotlin-android`
}

description = "Android instrumented testing MockK inline mocking agent"

val byteBuddyVersion = "1.12.10"
val objenesisVersion = "3.2"
val dexmakerVersion = "2.28.1"


dependencies {
//    api(project(":${mockKProject.name}")) {
//        exclude(group = "io.mockk", module = "mockk-agent-jvm")
//    }
    implementation(project(":modules:mockk-agent-android"))
    implementation(project(":modules:mockk-agent-api"))

    testImplementation("junit:junit:4.13.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    androidTestImplementation(kotlin("reflect"))
    androidTestImplementation(Deps.Libs.kotlinCoroutinesCore())
    androidTestImplementation(Deps.Libs.kotlinTestJunit()) {
        exclude(group = "junit", module = "junit")
    }
    androidTestImplementation("androidx.test:rules:1.4.0")

    androidTestImplementation(Deps.Libs.junitJupiterApi)
    androidTestImplementation(Deps.Libs.junitJupiterEngine)
    androidTestImplementation(Deps.Libs.junitVintageEngine)
}


//
//kotlin {
//    jvm {
//        withJava()
//    }
//
//    sourceSets {
//        val commonMain by getting {
//            dependencies {
//                api(projects.modules.mockkAgentApi)
//                api(projects.modules.mockkAgent)
//                implementation(kotlin("reflect"))
//            }
//        }
//        val commonTest by getting {
//            dependencies {
//            }
//        }
//        val jvmMain by getting {
//            dependencies {
////                api (project(":mockk-agent-common"))
//
//                api("org.objenesis:objenesis:$objenesisVersion")
//
//                api("net.bytebuddy:byte-buddy:$byteBuddyVersion")
//                api("net.bytebuddy:byte-buddy-agent:$byteBuddyVersion")
//            }
//        }
//        val jvmTest by getting {
//            dependencies {
//                implementation(buildsrc.config.Deps.Libs.junitJupiter)
//                implementation(buildsrc.config.Deps.Libs.junitVintageEngine)
//            }
//        }
//    }
//}
