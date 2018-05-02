package io.mockk

import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.*
import org.slf4j.LoggerFactory

interface Wrapper

class MockKTestSuite : StringSpec({
    val mock = mockk<MockCls>("mock")
    val spy = spyk(MockCls())
    val openMock = mockk<OpenMockCls>("mock")
    val log = LoggerFactory.getLogger(MockKTestSuite::class.java)

    fun expectVerificationError(vararg messages: String, block: () -> Unit) {
        try {
            clearMocks(mock, openMock)
            block()
            fail("Block should throw verification failure")
        } catch (ex: AssertionError) {
            log.info("Exception: {}", ex.message)
            if (messages.any { !ex.message!!.contains(it) }) {
                fail("Bad message: " + ex.message)
            }
        }
    }

    "verification outcome" {
    }.config(enabled = true)

    "coroutines" {
        coEvery { mock.coOtherOp(1, any()) } answers { 2 + firstArg<Int>() }

        runBlocking {
            mock.coOtherOp(1, 2)
        }

        coVerify { mock.coOtherOp(1, 2) }

        val slot = slot<suspend () -> Int>()
        coEvery { spy.coLambdaOp(1, capture(slot)) } answers {
            1 - slot.coInvoke()
        }

        runBlocking {
            spy.coLambdaOp(1, { 2 })
        }

        coVerify {
            spy.coLambdaOp(1, any())
        }
    }.config(enabled = true)


    "lambda functions" {
        every {
            mock.lambdaOp(1, captureLambda())
        } answers { 1 - lambda<() -> Int>().invoke() }

        assertEquals(-4, mock.lambdaOp(1, { 5 }))

        verify {
            mock.lambdaOp(1, any())
        }

        coEvery {
            mock.coLambdaOp(1, captureCoroutine())
        } answers { 1 - coroutine<suspend () -> Int>().coInvoke() }

        runBlocking {
            assertEquals(-4, mock.coLambdaOp(1, { 5 }))
        }

        coVerify {
            mock.lambdaOp(1, any())
        }
    }

    "extension functions" {
        staticMockk("io.mockk.MockKTestSuiteKt").use {
            every {
                IntWrapper(5).f()
            } returns 11

            assertEquals(11, IntWrapper(5).f())
            assertEquals(25, IntWrapper(20).f())

            verify {
                IntWrapper(5).f()
                IntWrapper(20).f()
            }
        }

        with(mockk<ExtObj>()) {
            every {
                IntWrapper(5).h()
            } returns 11

            assertEquals(11, IntWrapper(5).h())

            verify {
                IntWrapper(5).h()
            }
        }
    }.config(enabled = true)

    "spy" {
        val executed = arrayOf(false, false, false, false)
        val spyObj = spyk(SpyTest(executed)) // uncomment this as a semi-workaround

        every {
            spyObj.doSomething()
        } answers {
            callOriginal()
        }

        every {
            spyObj.computeSomething(1)
        } returns null

        every {
            spyObj.computeSomething(2)
        } answers { callOriginal()?.plus(4) }

        assertNotNull(spyObj.someReference)

        spyObj.doSomething()

        assertNull(spyObj.computeSomething(1))
        assertEquals(11, spyObj.computeSomething(2))
        assertEquals(8, spyObj.computeSomething(3))

        assertTrue(executed[0])
        assertTrue(executed[1])
        assertTrue(executed[2])
        assertTrue(executed[3])
    }.config(enabled = true)

    "varargs" {
        every { mock.varArgsOp(5, 6, 7, c = 8) } returns 1
        every { mock.varArgsOp(6, eq(3), 7, c = 8) } returns 2
        every { mock.varArgsOp(7, eq(3), any(), c = 8) } returns 3

        assertEquals(1, mock.varArgsOp(5, 6, 7, c = 8))
        assertEquals(2, mock.varArgsOp(6, 3, 7, c = 8))
        assertEquals(3, mock.varArgsOp(7, 3, 22, c = 8))

        val slot = slot<Int>()

        verify { mock.varArgsOp(5, 6, more(5), c = 8) }
        verify { mock.varArgsOp(6, any(), more(5), c = 8) }
        verify { mock.varArgsOp(7, capture(slot), more(20), c = 8) }

        assertEquals(3, slot.captured)

        val jvMmock = mockk<JvmVarArgsCls>()
        every { jvMmock.varArgsOp(5, 6, 7) } returns 1
        every { jvMmock.varArgsOp(6, eq(3), 7) } returns 2
        every { jvMmock.varArgsOp(7, eq(4), any()) } returns 3

        assertEquals(1, jvMmock.varArgsOp(5, 6, 7))
        assertEquals(2, jvMmock.varArgsOp(6, 3, 7))
        assertEquals(3, jvMmock.varArgsOp(7, 4, 22))

        verify { jvMmock.varArgsOp(5, 6, more(5)) }
        verify { jvMmock.varArgsOp(6, any(), more(5)) }
        verify { jvMmock.varArgsOp(7, capture(slot), more(20)) }

        assertEquals(4, slot.captured)


    }.config(enabled = true)
})

class ExtCls {
    fun IntWrapper.g() = data + 5
}

object ExtObj {
    fun IntWrapper.h() = data + 5
}

fun IntWrapper.f() = data + 5

data class IntWrapper(val data: Int) : Wrapper
data class DoubleWrapper(val data: Double) : Wrapper

class MockCls {
    fun manyArgsOp(
        a: Boolean = true, b: Boolean = true,
        c: Byte = 1, d: Byte = 2,
        e: Short = 3, f: Short = 4,
        g: Char = 5.toChar(), h: Char = 6.toChar(),
        i: Int = 7, j: Int = 8,
        k: Long = 9, l: Long = 10,
        m: Float = 10.0f, n: Float = 11.0f,
        o: Double = 12.0, p: Double = 13.0,
        q: String = "14", r: String = "15",
        s: IntWrapper = IntWrapper(16), t: IntWrapper = IntWrapper(17)
    ): Double {

        return (if (a) 0 else -1) + (if (b) 0 else -2) + c + d + e + f + g.toByte() + h.toByte() +
                i + j + k + l + m + n + o + p + q.toInt() + r.toInt() + s.data + t.data
    }

    fun lambdaOp(a: Int, b: () -> Int) = a + b()
    fun varArgsOp(a: Int, vararg b: Int, c: Int, d: Int = 6) = b.sum() + a
    suspend fun coLambdaOp(a: Int, b: suspend () -> Int) = a + b()
    suspend fun coOtherOp(a: Int = 1, b: Int = 2): Int = a + b
    fun otherOp(a: Wrapper? = IntWrapper(1), b: Wrapper? = IntWrapper(2)): Int {
        return if (a is IntWrapper && b is IntWrapper) {
            a.data + b.data
        } else {
            0
        }
    }

    fun nullableOp(a: Int = 1, b: Int = 2): Int? = a + b


    fun chainOp(a: Int = 1, b: Int = 2) = if (a + b > 0) MockCls() else MockCls()

    fun opNeverCalled(): Int = 1
}

abstract class OpenMockCls {
    abstract fun op(a: Int = 1, b: Int = 2): Int
}

open class BaseTest(val someReference: String, val executed: Array<Boolean>) {
    open fun doSomething() {
        executed[0] = true
    }

    open fun computeSomething(a: Int): Int? {
        executed[2] = true
        return null
    }
}

class SpyTest(executed: Array<Boolean>) : BaseTest("A spy", executed) {
    override fun doSomething() {
        executed[1] = true
        super.doSomething()
    }

    override fun computeSomething(a: Int): Int? {
        executed[3] = true
        super.computeSomething(a)
        return 5 + a
    }
}
