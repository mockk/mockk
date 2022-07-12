package io.mockk.dependencies

object Deps {
    object Versions {
        const val slfj = "_"
        const val logback = "_"
        const val junitJupiter = "_"
        const val junitVintage = "_"
    }

    object Libs {
        const val slfj = "org.slf4j:slf4j-api:${Versions.slfj}"
        const val logback = "ch.qos.logback:logback-classic:${Versions.logback}"
        const val junitJupiterApi = "org.junit.jupiter:junit-jupiter-api:${Versions.junitJupiter}"
        const val junitJupiterEngine =
            "org.junit.jupiter:junit-jupiter-engine:${Versions.junitJupiter}"
        const val junitVintageEngine =
            "org.junit.vintage:junit-vintage-engine:${Versions.junitVintage}"

        fun kotlinStdLib() = "org.jetbrains.kotlin:kotlin-stdlib:_"
        fun kotlinStdLibJs() = "org.jetbrains.kotlin:kotlin-stdlib-js:_"
        fun kotlinTestCommon() = "org.jetbrains.kotlin:kotlin-test-common:_"
        fun kotlinTestCommonAnnotations() = "org.jetbrains.kotlin:kotlin-test-annotations-common:_"

        fun kotlinTestJunit() = "org.jetbrains.kotlin:kotlin-test-junit:_"
        fun kotlinTestJs() = "org.jetbrains.kotlin:kotlin-test-js:_"
        fun kotlinReflect() = "org.jetbrains.kotlin:kotlin-reflect:_"
    }

}
