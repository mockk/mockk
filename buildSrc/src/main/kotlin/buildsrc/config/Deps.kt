package buildsrc.config

import org.gradle.api.JavaVersion
import org.gradle.api.Project

fun Project.kotlinVersion(): String =
    providers.gradleProperty("io_mockk_kotlin_version")
        .getOrElse(Deps.Versions.kotlinDefault)

object Deps {
    object Versions {
        val jvmTarget = JavaVersion.VERSION_1_8

        const val dokka = "1.9.0"
        const val kotlinDefault = "2.0.0"
        const val coroutines = "1.6.4"
        const val slfj = "2.0.5"
        const val logback = "1.4.5"
        const val junitJupiter = "5.10.2"
        const val junit4 = "4.13.2"
        const val assertj = "3.25.3"

        const val kotlinReflect = "1.7.21"

        const val springBoot = "3.2.2"
        const val spring = "6.1.3"

        const val byteBuddy = "1.14.17"
        const val objenesis = "3.3"
        const val dexmaker = "2.28.3"
        const val androidxEspresso = "3.5.1"
        const val androidxOrchestrator = "1.4.2"
        const val androidxTestRules = "1.5.0"
        const val androidxTestRunner = "1.5.2"
        const val androidxTestExtJunit = "1.1.5"
        const val androidxTestOrchestrator = "1.4.2"

        const val compileSdk = 34
        const val targetSdk = 34
        const val minSdk = 21
    }

    object Libs {
        const val slfj = "org.slf4j:slf4j-api:${Versions.slfj}"
        const val logback = "ch.qos.logback:logback-classic:${Versions.logback}"

        const val junit4 = "junit:junit:${Versions.junit4}"
        const val junitJupiter = "org.junit.jupiter:junit-jupiter:${Versions.junitJupiter}"
        const val junitJupiterParams = "org.junit.jupiter:junit-jupiter-params:${Versions.junitJupiter}"
        const val assertj = "org.assertj:assertj-core:${Versions.assertj}"

        const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlinReflect}"

        const val springBootTest = "org.springframework.boot:spring-boot-test:${Versions.springBoot}"
        const val springTest = "org.springframework:spring-test:${Versions.spring}"
        const val springContext = "org.springframework:spring-context:${Versions.spring}"

        const val kotlinCoroutinesBom = "org.jetbrains.kotlinx:kotlinx-coroutines-bom:${Versions.coroutines}"
        const val kotlinCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core"
        const val kotlinCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test"
    }
}
