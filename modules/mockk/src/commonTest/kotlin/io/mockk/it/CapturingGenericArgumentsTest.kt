package io.mockk.it

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.Test

class CapturingGenericArgumentsTest {
    @Test
    fun captureGenericArgument() {
        val mock = createMock<Int>()
        mock.tryBar(3)
    }

    interface Foo<in T> {
        fun bar(value: T)
    }

    private inline fun <reified T : Any> createMock(): Foo<Any> {
        val slot = slot<T>()

        return mockk {
            every {
                bar(capture(slot))
            } just Runs
        }
    }

    private fun <T> Foo<T>.tryBar(value: T) {
        bar(value)
    }
}
