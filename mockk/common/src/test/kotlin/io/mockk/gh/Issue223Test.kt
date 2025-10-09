package io.mockk.gh

import io.mockk.*
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore
class Issue223Test {
    interface Foo<in T> {
        fun bar(value: T)
    }

    @Test
    fun test() {
        val mock = createMock<Int>()
        mock.tryBar(3)
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
