package io.mockk.it

import io.mockk.*
import kotlinx.coroutines.coroutineScope
import kotlin.test.Test
import kotlin.test.assertEquals

class CoroutinesTest {
    private val mock = mockk<MockCls>()
    private val spy = spyk<MockCls>()

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
            spy.coLambdaOp(1) { 2 }
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
            assertEquals(-4, mock.coLambdaOp(1) { 5 })
        }

        coVerify {
            mock.coLambdaOp(1, any())
        }
    }

    /**
     * See issue #48
     */
    @Test
    fun mockPrivateCoroutineCall() {
        val myClassSpy = spyk<MockPrivateSuspendCls>(recordPrivateCalls = true)

        every { myClassSpy["myPrivateCall"](5) } returns "something"

        myClassSpy.publicCall()

        verify { myClassSpy["myPrivateCall"](5) }
    }

    class MockCls {
        suspend fun coOtherOp(a: Int = 1, b: Int = 2): Int = coroutineScope { a + b }
        suspend fun coLambdaOp(a: Int, b: suspend () -> Int) = a + b()
    }

    class MockPrivateSuspendCls {
        fun publicCall() {
            InternalPlatformDsl.runCoroutine {
                myPrivateCall(5)
            }
        }

        private suspend fun myPrivateCall(arg1: Int): Unit = coroutineScope { arg1.inc() }
    }
}
