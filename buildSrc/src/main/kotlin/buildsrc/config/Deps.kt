package buildsrc.config

import org.gradle.api.JavaVersion
import org.gradle.api.Project

fun Project.kotlinVersion() = findProperty("kotlin.version")?.toString() ?: Deps.Versions.kotlinDefault

object Deps {
    object Versions {
        val jvmTarget = JavaVersion.VERSION_1_8

        const val androidTools = "7.2.1"
        const val dokka = "1.7.10"
        const val kotlinDefault = "1.8.20"
        const val coroutines = "1.6.4"
        const val slfj = "2.0.5"
        const val logback = "1.4.5"
        const val junitJupiter = "5.8.2"
        const val junitVintage = "5.8.2"
        const val junit4 = "4.13.2"

        const val byteBuddy = "1.12.20"
        const val objenesis = "3.3"
        const val dexmaker = "2.28.3"
        const val androidxEspresso = "3.4.0"
        const val androidxTestRules = "1.4.0"
        const val androidxTestRunner = "1.4.0"
        const val androidxTestExtJunit = "1.1.3"
        const val androidxTestOrchestrator = "1.4.2"
    }

    object Libs {
        const val slfj = "org.slf4j:slf4j-api:${Versions.slfj}"
        const val logback = "ch.qos.logback:logback-classic:${Versions.logback}"

        const val kotlinBom = "org.jetbrains.kotlin:kotlin-bom:${Versions.kotlinDefault}"
        const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect"

        const val junit4 = "junit:junit:${Versions.junit4}"
        const val junitJupiter = "org.junit.jupiter:junit-jupiter:${Versions.junitJupiter}"
        const val junitJupiterApi = "org.junit.jupiter:junit-jupiter-api:${Versions.junitJupiter}"
        const val junitJupiterEngine = "org.junit.jupiter:junit-jupiter-engine:${Versions.junitJupiter}"
        const val junitVintageEngine = "org.junit.vintage:junit-vintage-engine:${Versions.junitJupiter}"

        const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlinDefault}"
        const val kotlinTestJunit5 = "org.jetbrains.kotlin:kotlin-test-junit5:${Versions.kotlinDefault}"

        const val kotlinCoroutinesBom = "org.jetbrains.kotlinx:kotlinx-coroutines-bom:${Versions.coroutines}"
        const val kotlinCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core"
        const val kotlinCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test"
    }
}
