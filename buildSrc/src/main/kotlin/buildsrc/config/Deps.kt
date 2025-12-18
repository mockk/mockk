package buildsrc.config

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

object Deps {
    object Versions {
        val jvmTarget = JavaVersion.VERSION_1_8
        val kotlinCompatibility = KotlinVersion.KOTLIN_1_8

        const val ANDROID_SDK = 36
        const val ANDROID_MIN_SDK = 21
        // SDK 26+: Open classes, abstract classes, subclass proxies
        // SDK 28+: Final classes/methods, static mocking

        const val ANDROID_TEST_ESPRESSO = "3.7.0"
        const val ANDROID_TEST_ORCHESTRATOR = "1.6.1"
        const val ANDROID_TEST_RULES = "1.7.0"
        const val ANDROID_TEST_RUNNER = "1.7.0"
        const val ANDROID_TEST_JUNIT_EXTENSIONS = "1.3.0"
    }
}
