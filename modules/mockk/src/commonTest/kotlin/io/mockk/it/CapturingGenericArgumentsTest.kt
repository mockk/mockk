package io.mockk.it

import io.mockk.*
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore
class CapturingGenericArgumentsTest {
    /**
     * Unable to capture arguments passed to generic functions.
     * Verifies issue #223.
     */
    @Test
    fun captureGenericArgument() {
        val mock = createMock<Int>()
        mock.tryBar(3)
    }

    interface Foo<in T> {
        fun bar(value: T)
    }

    inline fun <reified T : Any> createMock(): Foo<Any> {
        val slot = slot<T>()

        return mockk {
            every {
                bar(capture(slot))
            } just Runs
        }
    }

    fun <T> Foo<T>.tryBar(value: T) {
        bar(value)
    }
}
