package io.mockk.it

import io.mockk.*
import io.mockk.impl.InternalPlatform
import kotlin.test.Test
import kotlin.test.assertEquals

class CoroutinesTest {
    val mock = mockk<MockCls>()
    val spy = spyk<MockCls>()

    @Test
    fun simpleCoroutineCall() {
        coEvery { mock.coOtherOp(1, any()) } answers { 2 + firstArg<Int>() }

        InternalPlatformDsl.runCoroutine {
            mock.coOtherOp(1, 2)
        }

        coVerify { mock.coOtherOp(1, 2) }
    }

    @Test
    fun coroutineLambdaSlot() {
        val slot = slot<suspend () -> Int>()
        coEvery { spy.coLambdaOp(1, capture(slot)) } answers {
            1 - slot.coInvoke()
        }

        InternalPlatformDsl.runCoroutine {
            spy.coLambdaOp(1, { 2 })
        }

        coVerify {
            spy.coLambdaOp(1, any())
        }
    }

    @Test
    fun coroutineLambdaInvoke() {
        coEvery {
            mock.coLambdaOp(1, captureCoroutine())
        } answers { 1 - coroutine<suspend () -> Int>().coInvoke() }

        InternalPlatformDsl.runCoroutine {
            assertEquals(-4, mock.coLambdaOp(1, { 5 }))
        }

        coVerify {
            mock.coLambdaOp(1, any())
        }
    }

    class MockCls {
        suspend fun coOtherOp(a: Int = 1, b: Int = 2): Int = a + b
        suspend fun coLambdaOp(a: Int, b: suspend () -> Int) = a + b()
    }
}