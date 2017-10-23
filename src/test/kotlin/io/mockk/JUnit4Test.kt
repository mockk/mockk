package io.mockk

import io.mockk.external.MockKJUnitRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(MockKJUnitRunner::class)
class JUnit4Test {
    @Test
    fun test() {
        mockk<CLS>()
        println("Bye!")
    }

    class CLS {
        fun f(a: Int, b: Int) = a + b
    }
}
