package io.mockk.it

import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class LambdaTest {
    private val mock = mockk<MockCls>()

    @Test
    fun simpleLambdaTest() {
        every {
            mock.lambdaOp(1, captureLambda())
        } answers { 1 - lambda<() -> Int>().invoke() }

        assertEquals(-4, mock.lambdaOp(1) { 5 })

        verify {
            mock.lambdaOp(1, any())
        }
    }

    class MockCls {
        fun lambdaOp(a: Int, b: () -> Int) = a + b()
    }
}
