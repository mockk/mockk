package io.mockk.it

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Standalone entry point executed in a fresh JVM by [StaticMockFilesTest] (issue #1402).
 *
 * The kotlin-reflect static initialization this repro exercises happens exactly once per JVM,
 * so the failure is only observable in a JVM where nothing ran before: in the shared test
 * worker any earlier `every { ... }` already initializes kotlin-reflect and the regression
 * stays hidden.
 */
object StaticMockFilesRepro {
    @JvmStatic
    fun main(args: Array<String>) {
        mockkStatic(Files::class)
        every { Files.size(any()) } returns 99L

        val size = Files.size(Path.of("some-file"))

        unmockkStatic(Files::class)

        check(size == 99L) { "Expected mocked size 99L, got $size" }
        println("STATIC FILES MOCK OK")
    }
}

/**
 * GitHub issue #1402
 *
 * `mockkStatic(java.nio.file.Files::class)` used to crash with a kotlin-reflect
 * `ExceptionInInitializerError` (NPE inside `NewKotlinTypeChecker` initialization) on the
 * first `every { Files... }` recording: kotlin-reflect initializes its type-checker
 * machinery through a `ServiceLoader` scan that reads the classpath, and on JDK 9+ that
 * scan opens classpath jars through `ZipFile`, which itself calls `Files.readAttributes`.
 * With `Files` already instrumented, that JVM-internal call re-entered the mock handler,
 * which re-entered kotlin-reflect while it was still mid-initialization.
 *
 * The test runs the repro in a fresh JVM so kotlin-reflect is guaranteed to be
 * uninitialized when `mockkStatic(Files::class)` opens the interception window, matching
 * what happens in a user's test process.
 */
class StaticMockFilesTest {
    @Test
    fun mockkStaticFilesClassAndRecording() {
        val classpath =
            System.getProperty("io.mockk.test.classpath")
                ?: error(
                    "io.mockk.test.classpath system property is not set. " +
                        "It must point to the test runtime classpath so this test can fork a JVM.",
                )

        val javaHome = System.getProperty("java.home")
        val javaExecutable =
            Path
                .of(javaHome, "bin", if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java")
                .toString()

        val process =
            ProcessBuilder(javaExecutable, "-cp", classpath, "io.mockk.it.StaticMockFilesRepro")
                .redirectErrorStream(true)
                .start()

        val output =
            process.inputStream.readAllBytes().decodeToString()

        assertTrue(
            process.waitFor(5, TimeUnit.MINUTES),
            "Child JVM did not finish in time. Output:\n$output",
        )

        assertEquals(0, process.exitValue(), "Child JVM failed. Output:\n$output")
        assertTrue("STATIC FILES MOCK OK" in output, "Repro did not report success. Output:\n$output")
    }
}
