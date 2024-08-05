package io.mockk.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.TextFormat
import io.mockk.lint.MockkUnnecessaryUsageDetector.Companion.ISSUE_DATA_CLASS
import io.mockk.lint.MockkUnnecessaryUsageDetector.Companion.ISSUE_ENUM
import io.mockk.lint.MockkUnnecessaryUsageDetector.Companion.ISSUE_INTERFACE
import io.mockk.lint.MockkUnnecessaryUsageDetector.Companion.ISSUE_PRIMITIVE
import kotlin.test.Test

class MockkUnnecessaryUsageDetectorTest {

    private val Issue.explanation get() = getExplanation(TextFormat.TEXT)
    private val Issue.severity get() = defaultSeverity.description

    @Test
    fun expectWarningsForDataClass(): Unit = with(ISSUE_DATA_CLASS) {
        lint().allowMissingSdk()
            .files(
                kotlin(
                    """
                    import io.mockk.mockk
                    data class Data(val any: Any)
                    val data1 = mockk<Data>()
                    val data2: Data = mockk()
                    """,
                ).indented(),
                MOCKK_STUB,
            )
            .issues(this)
            .run()
            .expectWarningCount(2)
            .expectContains(
                """
                src/Data.kt:3: $severity: $explanation [$id]
                val data1 = mockk<Data>()
                            ~~~~~~~~~~~~~
                src/Data.kt:4: $severity: $explanation [$id]
                val data2: Data = mockk()
                                  ~~~~~~~
                0 errors, 2 warnings
            """.trimIndent(),
            )
    }

    @Test
    fun expectWarningsForEnum(): Unit = with(ISSUE_ENUM) {
        lint().allowMissingSdk()
            .files(
                kotlin(
                    """
                    import io.mockk.mockk

                    enum class Color { RED, GREEN, BLUE }

                    val explicit = mockk<Color>()
                    val inferred: Color = mockk()
                    """,
                ).indented(),
                MOCKK_STUB,
            )
            .issues(this)
            .run()
            .expectWarningCount(2)
            .expectContains(
                """
                src/Color.kt:5: $severity: $explanation [$id]
                val explicit = mockk<Color>()
                               ~~~~~~~~~~~~~~
                src/Color.kt:6: $severity: $explanation [$id]
                val inferred: Color = mockk()
                                      ~~~~~~~
                0 errors, 2 warnings
            """.trimIndent(),
            )
    }

    @Test
    fun expectWarningsForInterface(): Unit = with(ISSUE_INTERFACE) {
        lint().allowMissingSdk()
            .files(
                kotlin(
                    """
                    import io.mockk.mockk

                    interface Repository {
                        fun get()
                    }

                    val explicit = mockk<Repository>()
                    val inferred: Repository = mockk()
                    """,
                ).indented(),
                MOCKK_STUB,
            )
            .issues(this)
            .run()
            .expectWarningCount(2)
            .expectContains(
                """
                src/Repository.kt:7: $severity: $explanation [$id]
                val explicit = mockk<Repository>()
                               ~~~~~~~~~~~~~~~~~~~
                src/Repository.kt:8: $severity: $explanation [$id]
                val inferred: Repository = mockk()
                                           ~~~~~~~
                0 errors, 2 warnings
            """.trimIndent(),
            )
    }

    @Test
    fun expectWarningsForPrimitive(): Unit = with(ISSUE_PRIMITIVE) {
        lint().allowMissingSdk()
            .files(
                kotlin(
                    """
                    import io.mockk.mockk

                    val integer = mockk<Integer>()
                    val short = mockk<Short>()
                    val long = mockk<Long>()
                    val string = mockk<String>()
                    """,
                ).indented(),
                MOCKK_STUB,
            )
            .issues(this)
            .run()
            .expectWarningCount(4)
            .expectContains(
                """
                src/test.kt:3: $severity: $explanation [$id]
                val integer = mockk<Integer>()
                              ~~~~~~~~~~~~~~~~
                src/test.kt:4: $severity: $explanation [$id]
                val short = mockk<Short>()
                            ~~~~~~~~~~~~~~
                src/test.kt:5: $severity: $explanation [$id]
                val long = mockk<Long>()
                           ~~~~~~~~~~~~~
                src/test.kt:6: $severity: $explanation [$id]
                val string = mockk<String>()
                             ~~~~~~~~~~~~~~~
                0 errors, 4 warnings
            """.trimIndent(),
            )
    }

    @Test
    fun expectWarningsForValueClass(): Unit = with(ISSUE_PRIMITIVE) {
        lint().allowMissingSdk()
            .files(
                kotlin(
                    """
                    import io.mockk.mockk

                    @JvmInline
                    value class Password(private val p: String)

                    val password = mockk<Password>()
                    """,
                ).indented(),
                MOCKK_STUB,
            )
            .issues(this)
            .run()
            .expectWarningCount(1)
            .expectContains(
                """
                src/Password.kt:6: $severity: $explanation [$id]
                val password = mockk<Password>()
                               ~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
            """.trimIndent(),
            )
    }

    @Test
    fun expectClean() {
        lint().allowMissingSdk()
            .files(
                kotlin(
                    """
                    import io.mockk.mockk

                    class Data(val any: Any)
                    val explicit: Data = mockk()
                    val inferred = mockk<Data>()
                    """,
                ).indented(),
                MOCKK_STUB,
            )
            .detector(MockkUnnecessaryUsageDetector())
            .run()
            .expectClean()
    }

    private companion object {

        private val MOCKK_STUB: TestFile = kotlin(
            /*"MockK.kt",*/
            """
                package io.mockk

                import kotlin.reflect.KClass

                inline fun <reified T : Any> mockk(vararg any: Any?): T = TODO()
                """,
        ).indented()

    }

}
