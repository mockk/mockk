package io.mockk.test

import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Test
import java.util.function.Consumer
import kotlin.test.assertEquals

/**
 * Test for issue: java.base does not "opens java.util.function" since 1.14.0
 *
 * This test validates that spying on JDK functional interfaces (like Consumer)
 * works correctly on JDK 16+ without requiring --add-opens JVM arguments.
 *
 * The problem occurs when MockK tries to make internal JDK lambda methods accessible
 * (via `method.isAccessible = true`) which is not permitted on JDK 16+ due to
 * the Java module system's strong encapsulation.
 */
class JdkConsumerSpyTest {
    @Test
    fun `spying on Consumer should work without InaccessibleObjectException`() {
        val results = mutableListOf<String>()

        val consumer =
            spyk<Consumer<String>> {
                every { this@spyk.accept(any()) } answers { results.add(firstArg<String>()) }
            }

        consumer.accept("test1")
        consumer.accept("test2")

        assertEquals(listOf("test1", "test2"), results)
    }

    @Test
    fun `spying on Consumer and using andThen should work`() {
        val results = mutableListOf<String>()

        val consumer1: Consumer<String> =
            spyk<Consumer<String>> {
                every { this@spyk.accept(any()) } answers { results.add("first: ${firstArg<String>()}") }
            }

        val consumer2: Consumer<String> = Consumer { results.add("second: $it") }

        // This is the pattern that triggered the original issue
        // Consumer.andThen creates an internal lambda that MockK may try to access
        val combined = consumer1.andThen(consumer2)

        combined.accept("value")

        assertEquals(listOf("first: value", "second: value"), results)
    }
}
