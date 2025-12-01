package buildsrc.config

import org.gradle.api.JavaVersion
import org.gradle.api.Project

fun Project.kotlinVersion(): String =
    providers.gradleProperty("io_mockk_kotlin_version")
        .getOrElse(Deps.Versions.kotlinDefault)

object Deps {
    object Versions {
        val jvmTarget = JavaVersion.VERSION_1_8

        const val dokka = "2.0.0"
        const val kotlinDefault = "2.1.20"
        const val coroutines = "1.10.1"
        const val slfj = "2.0.17"
        const val logback = "1.5.18"
        const val junitJupiter = "5.12.2"
        const val junit4 = "4.13.2"
        const val assertj = "3.25.3"

        const val kotlinReflect = "1.7.21"

        const val springBoot4 = "4.0.0"
        const val spring7 = "7.0.1"

        const val springBoot3 = "3.2.2"
        const val spring6 = "6.1.3"

        const val springBoot2 = "2.7.18"
        const val spring5 = "5.3.39"

        const val byteBuddy = "1.15.11"
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
        const val minSdk = 26
    }

    object Libs {
        const val slfj = "org.slf4j:slf4j-api:${Versions.slfj}"
        const val logback = "ch.qos.logback:logback-classic:${Versions.logback}"

        const val junit4 = "junit:junit:${Versions.junit4}"
        const val junitJupiter = "org.junit.jupiter:junit-jupiter:${Versions.junitJupiter}"
        const val junitJupiterParams = "org.junit.jupiter:junit-jupiter-params:${Versions.junitJupiter}"
        const val assertj = "org.assertj:assertj-core:${Versions.assertj}"

        const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlinReflect}"

        const val springBoot4Test = "org.springframework.boot:spring-boot-test:${Versions.springBoot4}"
        const val spring7Test = "org.springframework:spring-test:${Versions.spring7}"
        const val spring7Context = "org.springframework:spring-context:${Versions.spring7}"

        const val springBoot3Test = "org.springframework.boot:spring-boot-test:${Versions.springBoot3}"
        const val spring6Test = "org.springframework:spring-test:${Versions.spring6}"
        const val spring6Context = "org.springframework:spring-context:${Versions.spring6}"

        const val springBoot2Test = "org.springframework.boot:spring-boot-test:${Versions.springBoot2}"
        const val spring5Test = "org.springframework:spring-test:${Versions.spring5}"
        const val spring5Context = "org.springframework:spring-context:${Versions.spring5}"

        const val kotlinCoroutinesBom = "org.jetbrains.kotlinx:kotlinx-coroutines-bom:${Versions.coroutines}"
        const val kotlinCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core"
        const val kotlinCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test"
    }
}
