package buildsrc.convention

import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain


description = "Set JavaToolchain for compiling main and test code"


// Retrieve the JavaToolchainService extension
val javaToolchains: JavaToolchainService = extensions.getByType()


val javaToolchainMainVersion = javaLanguageVersion("javaToolchainMainVersion")
val javaToolchainTestVersion = javaLanguageVersion("javaToolchainTestVersion")


// The Java Toolchain compiler that should be used to compile *main* code
val javaToolchainMainCompiler: Provider<JavaCompiler> =
    javaToolchains.compilerFor {
        languageVersion.set(javaToolchainMainVersion)
    }

val javaToolchainMainLauncher: Provider<JavaLauncher> =
    javaToolchains.launcherFor {
        languageVersion.set(javaToolchainMainVersion)
    }


// The Java Toolchain that should be used to compile *test* code
val javaToolchainTestCompiler: Provider<JavaCompiler> =
    javaToolchains.compilerFor {
        languageVersion.set(javaToolchainTestVersion)
    }


val javaToolchainTestLauncher: Provider<JavaLauncher> =
    javaToolchains.launcherFor {
        languageVersion.set(javaToolchainTestVersion)
    }


plugins.withType<KotlinBasePlugin>().configureEach {
    tasks.withType<UsesKotlinJavaToolchain>()
        .matching { it.name.contains("main", ignoreCase = true) }
        .configureEach {
            kotlinJavaToolchain.toolchain.use(javaToolchainMainLauncher)
        }
    tasks.withType<UsesKotlinJavaToolchain>()
        .matching { it.name.contains("test", ignoreCase = true) }
        .configureEach {
            kotlinJavaToolchain.toolchain.use(javaToolchainTestLauncher)
        }
}


plugins.withType<JavaBasePlugin>().configureEach {
    tasks.withType<JavaCompile>()
        .matching { it.name.contains("main", ignoreCase = true) }
        .configureEach {
            javaCompiler.set(javaToolchainMainCompiler)
        }

    tasks.withType<JavaCompile>()
        .matching { it.name.contains("test", ignoreCase = true) }
        .configureEach {
            javaCompiler.set(javaToolchainTestCompiler)
        }
}

/** helper function to create JavaLanguageVersion object from a Gradle property */
fun Project.javaLanguageVersion(
    gradleProperty: String
): Provider<JavaLanguageVersion> =
    providers.gradleProperty(gradleProperty)
        .map { JavaLanguageVersion.of(it) }
