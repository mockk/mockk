import buildsrc.config.Deps

plugins {
    buildsrc.convention.`kotlin-jvm`
}

dependencies {
    compileOnly(Deps.Libs.lintApi)
    compileOnly(Deps.Libs.lintChecks)

    testImplementation(Deps.Libs.junitJupiter)
    testImplementation(Deps.Libs.lintApi)
    testImplementation(Deps.Libs.lintTests)
    testImplementation(kotlin("test"))
}
