import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import kotlinx.benchmark.gradle.benchmark

plugins {
    buildsrc.convention.`kotlin-jvm`
    alias(libs.plugins.kotlinx.benchmark)
}

dependencies {
    implementation(libs.kotlinx.benchmark.runtime)
    implementation(platform(libs.kotlin.coroutines.bom))
    implementation(libs.kotlin.coroutines.core)
    implementation(projects.modules.mockk)
}

benchmark {
    configurations {
        named("main") {
            iterationTime = 10
            iterationTimeUnit = "sec"
            iterations = 3
            warmups = 1
        }
    }
    targets {
        register("main") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.37"
        }
    }
}
