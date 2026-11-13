package io.mockk.test

import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.function.Consumer
import kotlin.test.assertEquals

/**
 * Regression tests for spying on lambdas, which fail with
 * `NoClassDefFoundError: f/q/ClassName$$Lambda$13/0x000000013131313` on Kotlin 2
 * (https://github.com/mockk/mockk/issues/1313).
 *
 * Since Kotlin 2.0 lambdas are compiled with `invokedynamic`, and their classes are VM hidden
 * classes (`Outer$$Lambda$N/0x...`) that can be neither subclassed nor instrumented. On Kotlin 1.x
 * the same tests exercise the plain synthetic lambda classes.
 */
class LambdaSpyTest {
    @Test
    fun `spying on a non-capturing Kotlin lambda`() {
        val original: () -> String = { "original" }

        val spied = spyk(original)

        assertEquals("original", spied())
    }

    @Test
    fun `spied Kotlin lambda is verifiable`() {
        val callback = spyk({})

        callback()
        callback()

        verify { callback() }
    }

    @Test
    fun `stubbing a call on a spied Kotlin lambda`() {
        val original: () -> String = { "original" }

        val spied = spyk(original)
        every { spied() } returns "stubbed"

        assertEquals("stubbed", spied())
    }

    @Test
    fun `spying on a capturing Kotlin lambda keeps the original behaviour`() {
        val prefix = "mr. "
        val original: (String) -> String = { prefix + it }

        val spied = spyk(original)

        assertEquals("mr. bob", spied("bob"))
    }

    @Test
    fun `spying on a Java lambda`() {
        val seen = mutableListOf<String>()
        val original: Consumer<String> = Consumer { seen.add(it) }

        val spied = spyk(original)

        spied.accept("ab")

        assertEquals(listOf("ab"), seen)
    }
}
