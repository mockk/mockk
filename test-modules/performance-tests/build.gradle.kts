import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import kotlinx.benchmark.gradle.benchmark

plugins {
    buildsrc.convention.`kotlin-jvm`
    id("org.jetbrains.kotlinx.benchmark") version "0.4.5"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation(projects.modules.mockk)
}

benchmark {
    configurations {
        named("main") {
            iterationTime = 60
            iterationTimeUnit = "sec"
            iterations = 2
            warmups = 1
        }
    }
    targets {
        register("main") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.35"
        }
    }
}
