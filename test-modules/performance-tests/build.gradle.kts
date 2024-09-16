import buildsrc.config.Deps
import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import kotlinx.benchmark.gradle.benchmark

plugins {
    buildsrc.convention.`kotlin-jvm`
    id("org.jetbrains.kotlinx.benchmark") version "0.4.12"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.12")
    implementation(platform(Deps.Libs.kotlinCoroutinesBom))
    implementation(Deps.Libs.kotlinCoroutinesCore)
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
