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

    "arrays" {
        every { mock.arrayOp(BooleanArray(3, { true })) } returns BooleanArray(3, { false })
        every { mock.arrayOp(ByteArray(3, { (it + 1).toByte() })) } returns ByteArray(3, { (3 - it).toByte() })
        every { mock.arrayOp(ShortArray(3, { (it + 1).toShort() })) } returns ShortArray(3, { (3 - it).toShort() })
        every { mock.arrayOp(CharArray(3, { (it + 1).toChar() })) } returns CharArray(3, { (3 - it).toChar() })
        every { mock.arrayOp(IntArray(3, { it + 1 })) } returns IntArray(3, { 3 - it })
        every { mock.arrayOp(LongArray(3, { (it + 1).toLong() })) } returns LongArray(3, { (3 - it).toLong() })
        every { mock.arrayOp(FloatArray(3, { (it + 1).toFloat() })) } returns FloatArray(3, { (3 - it).toFloat() })
        every { mock.arrayOp(DoubleArray(3, { (it + 1).toDouble() })) } returns DoubleArray(3, { (3 - it).toDouble() })

        every { mock.arrayOp(Array<Boolean>(3, { true })) } returns Array<Boolean>(3, { false })
        every { mock.arrayOp(Array<Byte>(3, { (it + 1).toByte() })) } returns Array<Byte>(3, { (3 - it).toByte() })
        every { mock.arrayOp(Array<Short>(3, { (it + 1).toShort() })) } returns Array<Short>(3, { (3 - it).toShort() })
        every { mock.arrayOp(Array<Char>(3, { (it + 1).toChar() })) } returns Array<Char>(3, { (3 - it).toChar() })
        every { mock.arrayOp(Array<Int>(3, { it + 1 })) } returns Array<Int>(3, { 3 - it })
        every { mock.arrayOp(Array<Long>(3, { (it + 1).toLong() })) } returns Array<Long>(3, { (3 - it).toLong() })
        every { mock.arrayOp(Array<Float>(3, { (it + 1).toFloat() })) } returns Array<Float>(3, { (3 - it).toFloat() })
        every { mock.arrayOp(Array<Double>(3, { (it + 1).toDouble() })) } returns Array<Double>(
            3,
            { (3 - it).toDouble() })

        every { mock.arrayOp(Array<Any>(3, { it + 1 })) } returns Array<Any>(3, { 3 - it })
        every {
            mock.arrayOp(
                Array<Array<Any>>(
                    3,
                    { i -> Array<Any>(3, { j -> i + j }) })
            )
        } returns Array<Array<Any>>(3, { i -> Array<Any>(3, { j -> j - i }) })
        every { mock.arrayOp(any<Array<IntWrapper>>()) } answers { Array(3, { IntWrapper(it + 2) }) }

        assertArrayEquals(BooleanArray(3, { false }), mock.arrayOp(BooleanArray(3, { true })))
        assertArrayEquals(ByteArray(3, { (3 - it).toByte() }), mock.arrayOp(ByteArray(3, { (it + 1).toByte() })))
        assertArrayEquals(ShortArray(3, { (3 - it).toShort() }), mock.arrayOp(ShortArray(3, { (it + 1).toShort() })))
        assertArrayEquals(CharArray(3, { (3 - it).toChar() }), mock.arrayOp(CharArray(3, { (it + 1).toChar() })))
        assertArrayEquals(IntArray(3, { 3 - it }), mock.arrayOp(IntArray(3, { it + 1 })))
        assertArrayEquals(LongArray(3, { (3 - it).toLong() }), mock.arrayOp(LongArray(3, { (it + 1).toLong() })))
        assertArrayEquals(
            FloatArray(3, { (3 - it).toFloat() }),
            mock.arrayOp(FloatArray(3, { (it + 1).toFloat() })),
            1e-6f
        )
        assertArrayEquals(
            DoubleArray(3, { (3 - it).toDouble() }),
            mock.arrayOp(DoubleArray(3, { (it + 1).toDouble() })),
            1e-6
        )

        assertArrayEquals(Array<Boolean>(3, { false }), mock.arrayOp(Array<Boolean>(3, { true })))
        assertArrayEquals(Array<Byte>(3, { (3 - it).toByte() }), mock.arrayOp(Array<Byte>(3, { (it + 1).toByte() })))
        assertArrayEquals(
            Array<Short>(3, { (3 - it).toShort() }),
            mock.arrayOp(Array<Short>(3, { (it + 1).toShort() }))
        )
        assertArrayEquals(Array<Char>(3, { (3 - it).toChar() }), mock.arrayOp(Array<Char>(3, { (it + 1).toChar() })))
        assertArrayEquals(Array<Int>(3, { 3 - it }), mock.arrayOp(Array<Int>(3, { it + 1 })))
        assertArrayEquals(Array<Long>(3, { (3 - it).toLong() }), mock.arrayOp(Array<Long>(3, { (it + 1).toLong() })))
        assertArrayEquals(
            Array<Float>(3, { (3 - it).toFloat() }),
            mock.arrayOp(Array<Float>(3, { (it + 1).toFloat() }))
        )
        assertArrayEquals(Array<Double>(3, { (3 - it).toDouble() }),
            mock.arrayOp(Array<Double>(3, { (it + 1).toDouble() }))
        )

        assertArrayEquals(Array<Any>(3, { 3 - it }), mock.arrayOp(Array<Any>(3, { it + 1 })))
        assertArrayEquals(
            Array<Array<Any>>(3, { i -> Array<Any>(3, { j -> j - i }) }),
            mock.arrayOp(Array<Array<Any>>(3, { i -> Array<Any>(3, { j -> i + j }) }))
        )
        assertArrayEquals(Array(3, { IntWrapper(it + 2) }),
            mock.arrayOp(Array(3, { IntWrapper(it + 5) }))
        )

        verify { mock.arrayOp(BooleanArray(3, { true })) }
        verify { mock.arrayOp(ByteArray(3, { (it + 1).toByte() })) }
        verify { mock.arrayOp(ShortArray(3, { (it + 1).toShort() })) }
        verify { mock.arrayOp(CharArray(3, { (it + 1).toChar() })) }
        verify { mock.arrayOp(IntArray(3, { it + 1 })) }
        verify { mock.arrayOp(LongArray(3, { (it + 1).toLong() })) }
        verify { mock.arrayOp(FloatArray(3, { (it + 1).toFloat() })) }
        verify { mock.arrayOp(DoubleArray(3, { (it + 1).toDouble() })) }

        verify { mock.arrayOp(Array<Boolean>(3, { true })) }
        verify { mock.arrayOp(Array<Byte>(3, { (it + 1).toByte() })) }
        verify { mock.arrayOp(Array<Short>(3, { (it + 1).toShort() })) }
        verify { mock.arrayOp(Array<Char>(3, { (it + 1).toChar() })) }
        verify { mock.arrayOp(Array<Int>(3, { it + 1 })) }
        verify { mock.arrayOp(Array<Long>(3, { (it + 1).toLong() })) }
        verify { mock.arrayOp(Array<Float>(3, { (it + 1).toFloat() })) }
        verify { mock.arrayOp(Array<Double>(3, { (it + 1).toDouble() })) }

        verify { mock.arrayOp(Array<Any>(3, { it + 1 })) }
        verify { mock.arrayOp(Array<Array<Any>>(3, { i -> Array<Any>(3, { j -> i + j }) })) }

        verify { mock.arrayOp(Array(3, { IntWrapper(it + 5) })) }
    }.config(enabled = true)

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
        expectVerificationError(
            "Only one matching call to ",
            "but arguments are not matching",
            "MockCls.otherOp"
        ) {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)

            verify { mock.otherOp(1, 3) }
        }

        expectVerificationError("No matching calls found.", "Calls to same method", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 4)

            verify { mock.otherOp(1, 3) }
        }

        expectVerificationError("was not called", "Calls to same mock", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)

            verify { mock.manyArgsOp(true, false) }
        }

        expectVerificationError("was not called") {
            verify { mock.otherOp(1, 2) }
        }

        expectVerificationError("2 matching calls found, but needs at least 3 calls", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 2)

            verify(atLeast = 3) { mock.otherOp(1, 2) }
        }

        expectVerificationError("One matching call found, but needs at least 3 calls", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)

            verify(atLeast = 3) { mock.otherOp(1, 2) }
        }
        expectVerificationError("calls are not in verification order", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 3)

            verifyOrder {
                mock.otherOp(1, 3)
                mock.otherOp(1, 2)
            }
        }
        expectVerificationError("less calls happened then demanded by order verification sequence", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 3)

            verifyOrder {
                mock.otherOp(1, 3)
                mock.otherOp(1, 2)
            }
        }
        expectVerificationError("number of calls happened not matching exact number of verification sequence", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 3)

            verifySequence {
                mock.otherOp(1, 3)
                mock.otherOp(1, 2)
            }
        }
        expectVerificationError("calls are not exactly matching verification sequence", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 3)

            verifySequence {
                mock.otherOp(1, 3)
                mock.otherOp(1, 2)
            }
        }
        expectVerificationError("some calls were not matched", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 3)

            verifyAll {
                mock.otherOp(1, 2)
            }
        }
        expectVerificationError("MockCls(BB).op") {
            every { openMock.op(1, any()) } returns 3

            openMock.op(1, 2)
            openMock.op(1, 3)

            verifyAll {
                openMock.op(1, 2)
            }
        }
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

    fun otherOp(a: Int = 1, b: Int = 2): Int = a + b
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

    fun arrayOp(arr: BooleanArray) = arr.map { it }.toBooleanArray()
    fun arrayOp(arr: ByteArray) = arr.map { (it + 1).toByte() }.toByteArray()
    fun arrayOp(arr: ShortArray) = arr.map { (it + 1).toShort() }.toShortArray()
    fun arrayOp(arr: CharArray) = arr.map { (it + 1) }.toCharArray()
    fun arrayOp(arr: IntArray) = arr.map { (it + 1).toInt() }.toIntArray()
    fun arrayOp(arr: LongArray) = arr.map { (it + 1).toLong() }.toLongArray()
    fun arrayOp(arr: FloatArray) = arr.map { (it + 1).toFloat() }.toFloatArray()
    fun arrayOp(arr: DoubleArray) = arr.map { (it + 1).toDouble() }.toDoubleArray()

    fun arrayOp(arr: Array<Boolean>) = arr.map { it }.toTypedArray()
    fun arrayOp(arr: Array<Byte>) = arr.map { (it + 1).toByte() }.toTypedArray()
    fun arrayOp(arr: Array<Short>) = arr.map { (it + 1).toShort() }.toTypedArray()
    fun arrayOp(arr: Array<Char>) = arr.map { (it + 1).toChar() }.toTypedArray()
    fun arrayOp(arr: Array<Int>) = arr.map { (it + 1).toInt() }.toTypedArray()
    fun arrayOp(arr: Array<Long>) = arr.map { (it + 1).toLong() }.toTypedArray()
    fun arrayOp(arr: Array<Float>) = arr.map { (it + 1).toFloat() }.toTypedArray()
    fun arrayOp(arr: Array<Double>) = arr.map { (it + 1).toDouble() }.toTypedArray()

    fun chainOp(a: Int = 1, b: Int = 2) = if (a + b > 0) MockCls() else MockCls()
    fun arrayOp(array: Array<Any>): Array<Any> = array.map { (it as Int) + 1 }.toTypedArray()
    fun arrayOp(array: Array<Array<Any>>): Array<Array<Any>> =
        array.map { it.map { ((it as Int) + 1) as Any }.toTypedArray() }.toTypedArray()

    fun arrayOp(array: Array<IntWrapper>): Array<IntWrapper> = array.map { IntWrapper(it.data + 1) }.toTypedArray()

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
