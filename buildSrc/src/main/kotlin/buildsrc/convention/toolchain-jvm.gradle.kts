package buildsrc.convention

import buildsrc.config.Deps
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain


description = "Set JavaToolchain for compiling main and test code"


tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    sourceCompatibility = Deps.Versions.jvmTarget.toString()
    targetCompatibility = Deps.Versions.jvmTarget.toString()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = Deps.Versions.jvmTarget.toString()
    }
}

// Retrieve the JavaToolchainService extension
val javaToolchains: JavaToolchainService = extensions.getByType()


val javaToolchainMainVersion = javaLanguageVersion("io_mockk_java_toolchain_main_version")
val javaToolchainTestVersion = javaLanguageVersion("io_mockk_java_toolchain_test_version")


// The Java Toolchains that will compile/launch *main* code
val javaToolchainMainCompiler: Provider<JavaCompiler> =
    javaToolchains.compilerFor {
        languageVersion.set(javaToolchainMainVersion)
    }
val javaToolchainMainLauncher: Provider<JavaLauncher> =
    javaToolchains.launcherFor {
        languageVersion.set(javaToolchainMainVersion)
    }


// The Java Toolchains that wil compile/launch *test* code
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
