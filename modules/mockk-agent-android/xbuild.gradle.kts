plugins {
    buildsrc.convention.`kotlin-android`
}

description = "Android instrumented testing MockK inline mocking agent"

val byteBuddyVersion = "1.12.10"
val objenesisVersion = "3.2"
val dexmakerVersion = "2.28.1"


android {
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
        }
    }
}


dependencies {
    api(project(":modules:mockk-agent-api"))
    api(project(":modules:mockk-agent"))
    implementation("com.linkedin.dexmaker:dexmaker:$dexmakerVersion")
    implementation("org.objenesis:objenesis:$objenesisVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
//    , {
//        exclude group: 'com.android.support', module: 'support-annotations'
//    })
    androidTestImplementation("junit:junit:4.13.1")

//    implementation ("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation(kotlin("reflect"))
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
