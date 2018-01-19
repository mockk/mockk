package io.mockk

interface Wrapper

class MockKTestSuite : StringSpec({
    val mock = mockk<MockCls>("mock")
    val spy = spyk(MockCls())
    "partly argument matching" {
        every { mock.manyArgsOp(allAny()) } returns 0.0
        every { mock.manyArgsOp(a = eq(false)) } returns 1.0
        every { mock.manyArgsOp(b = eq(false)) } returns 2.0
        every { mock.manyArgsOp(c = eq(33)) } returns 3.0
        every { mock.manyArgsOp(d = eq(33)) } returns 4.0
        every { mock.manyArgsOp(e = eq(33)) } returns 5.0
        every { mock.manyArgsOp(f = eq(33)) } returns 6.0
        every { mock.manyArgsOp(g = eq(33.toChar())) } returns 7.0
        every { mock.manyArgsOp(h = eq(33.toChar())) } returns 8.0
        every { mock.manyArgsOp(i = eq(33)) } returns 9.0
        every { mock.manyArgsOp(j = eq(33)) } returns 10.0
        every { mock.manyArgsOp(k = eq(33)) } returns 11.0
        every { mock.manyArgsOp(l = eq(33)) } returns 12.0
        every { mock.manyArgsOp(m = eq(33.0f)) } returns 13.0
        every { mock.manyArgsOp(n = eq(33.0f)) } returns 14.0
        every { mock.manyArgsOp(o = eq(33.0)) } returns 15.0
        every { mock.manyArgsOp(p = eq(33.0)) } returns 16.0
        every { mock.manyArgsOp(q = eq("33")) } returns 17.0
        every { mock.manyArgsOp(r = eq("33")) } returns 18.0
        every { mock.manyArgsOp(s = eq(IntWrapper(33))) } returns 19.0
        every { mock.manyArgsOp(t = eq(IntWrapper(33))) } returns 20.0

        assertEquals(163.0, spy.manyArgsOp(), 1e-6)
        assertEquals(0.0, mock.manyArgsOp(), 1e-6)
        assertEquals(1.0, mock.manyArgsOp(a = false), 1e-6)
        assertEquals(2.0, mock.manyArgsOp(b = false), 1e-6)
        assertEquals(3.0, mock.manyArgsOp(c = 33), 1e-6)
        assertEquals(4.0, mock.manyArgsOp(d = 33), 1e-6)
        assertEquals(5.0, mock.manyArgsOp(e = 33), 1e-6)
        assertEquals(6.0, mock.manyArgsOp(f = 33), 1e-6)
        assertEquals(7.0, mock.manyArgsOp(g = 33.toChar()), 1e-6)
        assertEquals(8.0, mock.manyArgsOp(h = 33.toChar()), 1e-6)
        assertEquals(9.0, mock.manyArgsOp(i = 33), 1e-6)
        assertEquals(10.0, mock.manyArgsOp(j = 33), 1e-6)
        assertEquals(11.0, mock.manyArgsOp(k = 33), 1e-6)
        assertEquals(12.0, mock.manyArgsOp(l = 33), 1e-6)
        assertEquals(13.0, mock.manyArgsOp(m = 33.0f), 1e-6)
        assertEquals(14.0, mock.manyArgsOp(n = 33.0f), 1e-6)
        assertEquals(15.0, mock.manyArgsOp(o = 33.0), 1e-6)
        assertEquals(16.0, mock.manyArgsOp(p = 33.0), 1e-6)
        assertEquals(17.0, mock.manyArgsOp(q = "33"), 1e-6)
        assertEquals(18.0, mock.manyArgsOp(r = "33"), 1e-6)
        assertEquals(19.0, mock.manyArgsOp(s = IntWrapper(33)), 1e-6)
        assertEquals(20.0, mock.manyArgsOp(t = IntWrapper(33)), 1e-6)

        verify { mock.manyArgsOp(a = eq(false)) }
        verify { mock.manyArgsOp(b = eq(false)) }
        verify { mock.manyArgsOp(c = eq(33)) }
        verify { mock.manyArgsOp(d = eq(33)) }
        verify { mock.manyArgsOp(e = eq(33)) }
        verify { mock.manyArgsOp(f = eq(33)) }
        verify { mock.manyArgsOp(g = eq(33.toChar())) }
        verify { mock.manyArgsOp(h = eq(33.toChar())) }
        verify { mock.manyArgsOp(i = eq(33)) }
        verify { mock.manyArgsOp(j = eq(33)) }
        verify { mock.manyArgsOp(k = eq(33)) }
        verify { mock.manyArgsOp(l = eq(33)) }
        verify { mock.manyArgsOp(m = eq(33.0f)) }
        verify { mock.manyArgsOp(n = eq(33.0f)) }
        verify { mock.manyArgsOp(o = eq(33.0)) }
        verify { mock.manyArgsOp(p = eq(33.0)) }
        verify { mock.manyArgsOp(q = eq("33")) }
        verify { mock.manyArgsOp(r = eq("33")) }
        verify { mock.manyArgsOp(s = eq(IntWrapper(33))) }
        verify { mock.manyArgsOp(t = eq(IntWrapper(33))) }
    }

    "chained calls" {
        every { mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4) } returns 1
        every { mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8) } returns 2
        every { mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12) } returns 3

        assertEquals(1, mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4))
        assertEquals(2, mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8))
        assertEquals(3, mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12))

        verify {
            mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4)
            mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12)
            mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8)
        }
        verifyOrder {
            mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4)
            mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8)
        }
        verifyOrder {
            mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4)
            mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12)
        }
        verifyOrder {
            mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8)
            mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12)
        }
        verifySequence {
            mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4)
            mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8)
            mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12)
        }
        verifyAll {
            mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12)
            mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8)
            mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4)
        }
    }

    "clearMocks" {
        every { mock.otherOp(0, 2) } returns 5

        assertEquals(5, mock.otherOp(0, 2))
        clearMocks(mock, answers = false)
        assertEquals(5, mock.otherOp(0, 2))
        clearMocks(mock)
        every { mock.otherOp(any<Int>(), any<Int>()) } returns 0
        assertEquals(0, mock.otherOp(0, 2))

        verifySequence {
            mock.otherOp(0, 2)
        }
    }

    "atLeast, atMost, exactly, wasNot Called" {
        every { mock.otherOp(0, 2) } throws RuntimeException("test")
        every { mock.otherOp(1, 3) } returnsMany listOf(1, 2, 3)

        try {
            mock.otherOp(0, 2)
        } catch (ex: RuntimeException) {
            assertEquals("test", ex.message)
        }
        assertEquals(1, mock.otherOp(1, 3))
        assertEquals(2, mock.otherOp(1, 3))
        assertEquals(3, mock.otherOp(1, 3))
        assertEquals(3, mock.otherOp(1, 3))

        verify(atLeast = 4) {
            mock.otherOp(1, 3)
        }
        verify(atLeast = 5, inverse = true) {
            mock.otherOp(1, 3)
        }
        verify(exactly = 4) {
            mock.otherOp(1, 3)
        }
        verify(exactly = 3, inverse = true) {
            mock.otherOp(1, 3)
        }
        verify(atMost = 4) {
            mock.otherOp(1, 3)
        }
        verify(atMost = 3, inverse = true) {
            mock.otherOp(1, 3)
        }
        verify(exactly = 0) {
            mock.otherOp(1, 4)
        }
        verify(exactly = 1, inverse = true) {
            mock.otherOp(1, 4)
        }
        verify(exactly = 1) {
            mock.otherOp(0, 2)
        }
        verify(exactly = 2, inverse = true) {
            mock.otherOp(0, 2)
        }
        verify(exactly = 0, inverse = true) {
            mock.otherOp(0, 2)
        }
        verify(exactly = 0) {
            mock.opNeverCalled()
        }
        verifyAll(inverse = true) {
            mock.otherOp(0, 2)
        }
        val secondMock = mockk<MockCls>()
        val thirdMock = mockk<MockCls>()
        verify {
            listOf(secondMock, thirdMock) wasNot Called
        }
    }

    "stubbing actions" {
        every { mock.otherOp(0, 2) } throws RuntimeException("test")
        every { mock.otherOp(1, 3) } returnsMany listOf(1, 2, 3)

        try {
            mock.otherOp(0, 2)
        } catch (ex: RuntimeException) {
            assertEquals("test", ex.message)
        }
        assertEquals(1, mock.otherOp(1, 3))
        assertEquals(2, mock.otherOp(1, 3))
        assertEquals(3, mock.otherOp(1, 3))
        assertEquals(3, mock.otherOp(1, 3))

        verify { mock.otherOp(0, 2) }
        verifyOrder {
            mock.otherOp(1, 3)
            mock.otherOp(1, 3)
            mock.otherOp(1, 3)
            mock.otherOp(1, 3)
        }
        verifySequence {
            mock.otherOp(0, 2)
            mock.otherOp(1, 3)
            mock.otherOp(1, 3)
            mock.otherOp(1, 3)
            mock.otherOp(1, 3)
        }
    }

    "answers" {
        val lst = mutableListOf<Byte?>()
        val lstNonNull = mutableListOf<Byte>()
        val slot = slot<() -> Int>()

        every { spy.manyArgsOp(a = any(), c = 5) } answers { if (firstArg()) 1.0 else 2.0 }
        every { spy.manyArgsOp(b = any(), c = 6) } answers { if (secondArg()) 3.0 else 4.0 }
        every { spy.manyArgsOp(c = 7) } answers { thirdArg<Byte>().toDouble() - 2 }
        every { spy.manyArgsOp(t = any(), c = 8) } answers { lastArg<IntWrapper>().data.toDouble() }
        every { spy.manyArgsOp(c = 9) } answers { 20.0 }
        every { spy.manyArgsOp(c = 11) } answers { 20.0 }
        every { spy.manyArgsOp(d = capture(lstNonNull), c = 12) } answers { lstNonNull.captured().toDouble() }
        every { spy.manyArgsOp(d = captureNullable(lst), c = 13) } answers { lst.captured()!!.toDouble() }
        every { spy.lambdaOp(1, capture(slot)) } answers {
            1 - slot.invoke()
        }

        assertEquals(163.0, spy.manyArgsOp(), 1e-6)
        assertEquals(1.0, spy.manyArgsOp(true, c = 5), 1e-6)
        assertEquals(2.0, spy.manyArgsOp(false, c = 5), 1e-6)
        assertEquals(3.0, spy.manyArgsOp(b = true, c = 6), 1e-6)
        assertEquals(4.0, spy.manyArgsOp(b = false, c = 6), 1e-6)
        assertEquals(5.0, spy.manyArgsOp(c = 7), 1e-6)
        assertEquals(6.0, spy.manyArgsOp(c = 8, t = IntWrapper(6)), 1e-6)
        assertEquals(20.0, spy.manyArgsOp(c = 9), 1e-6)
        assertEquals(20.0, spy.manyArgsOp(c = 11), 1e-6)
        assertEquals(10.0, spy.manyArgsOp(d = 10, c = 12), 1e-6)
        assertEquals(11.0, spy.manyArgsOp(d = 11, c = 12), 1e-6)
        assertEquals(14.0, spy.manyArgsOp(d = 14, c = 13), 1e-6)
        assertEquals(-2, spy.lambdaOp(1, { 3 }))

        assertEquals(listOf(10.toByte(), 11.toByte()), lstNonNull)
        assertEquals(listOf(14.toByte()), lst)

        verify { spy.manyArgsOp() }
        verify { spy.manyArgsOp(true, c = 5) }
        verify { spy.manyArgsOp(false, c = 5) }
        verify { spy.manyArgsOp(b = true, c = 6) }
        verify { spy.manyArgsOp(b = false, c = 6) }
        verify { spy.manyArgsOp(c = 7) }
        verify { spy.manyArgsOp(c = 8, t = IntWrapper(6)) }
        verify { spy.manyArgsOp(c = 9) }
        verify { spy.manyArgsOp(c = 11) }
        verify { spy.manyArgsOp(d = 10, c = 12) }
        verify { spy.manyArgsOp(d = 11, c = 12) }
        verify { spy.manyArgsOp(d = 11, c = 12) }
        verify { spy.lambdaOp(1, assert { it.invoke() == 3 }) }
    }

    "verify, verifyOrder, verifySequence" {
        every { spy.manyArgsOp(c = 5) } returns 1.0
        every { spy.manyArgsOp(c = 6) } returns 2.0
        every { spy.manyArgsOp(c = 7) } returns 3.0

        assertEquals(1.0, spy.manyArgsOp(c = 5), 1e-6)
        assertEquals(2.0, spy.manyArgsOp(c = 6), 1e-6)
        assertEquals(3.0, spy.manyArgsOp(c = 7), 1e-6)

        verify {
            spy.manyArgsOp(c = 6)
            spy.manyArgsOp(c = 5)
        }
        verify(inverse = true) {
            spy.manyArgsOp(c = 6)
            spy.manyArgsOp(c = 8)
        }
        verify(inverse = true) {
            spy.manyArgsOp(c = 4)
            spy.manyArgsOp(c = 8)
        }

        verifyOrder {
            spy.manyArgsOp(c = 5)
            spy.manyArgsOp(c = 7)
        }
        verifyOrder {
            spy.manyArgsOp(c = 5)
            spy.manyArgsOp(c = 6)
        }
        verifyOrder {
            spy.manyArgsOp(c = 6)
            spy.manyArgsOp(c = 7)
        }
        verifyOrder(inverse = true) {
            spy.manyArgsOp(c = 7)
            spy.manyArgsOp(c = 5)
        }
        verifyOrder(inverse = true) {
            spy.manyArgsOp(c = 5)
            spy.manyArgsOp(c = 4)
        }
        verifyOrder(inverse = true) {
            spy.manyArgsOp(c = 4)
            spy.manyArgsOp(c = 8)
        }
        verifySequence {
            spy.manyArgsOp(c = 5)
            spy.manyArgsOp(c = 6)
            spy.manyArgsOp(c = 7)
        }
        verifySequence(inverse = true) {
            spy.manyArgsOp(c = 6)
            spy.manyArgsOp(c = 7)
        }
        verifySequence(inverse = true) {
            spy.manyArgsOp(c = 7)
            spy.manyArgsOp(c = 6)
            spy.manyArgsOp(c = 5)
        }
        verifySequence(inverse = true) {
            spy.manyArgsOp(c = 6)
            spy.manyArgsOp(c = 5)
            spy.manyArgsOp(c = 7)
        }
    }

    "matchers" {
        val a = IntWrapper(3)
        val b = IntWrapper(4)

        every { mock.otherOp(any<Int>(), any<Int>()) } returns 0
        every { mock.otherOp(any<IntWrapper>(), any<IntWrapper>()) } returns 0

        every { mock.otherOp(eq(a), refEq(b)) } returns 1

        every { mock.otherOp(1, less(2)) } returns 2
        every { mock.otherOp(1, cmpEq(2)) } returns 3
        every { mock.otherOp(1, more(2)) } returns 4

        every { mock.otherOp(2, less(1, andEquals = true)) } returns 5
        every { mock.otherOp(2, cmpEq(2)) } returns 6
        every { mock.otherOp(2, more(3, andEquals = true)) } returns 7

        every { mock.otherOp(3, or(eq(3), eq(5))) } returns 8
        every { mock.otherOp(3, and(more(8), less(15))) } returns 9
        every { mock.otherOp(3, or(more(20, andEquals = true), 17)) } returns 10
        every { mock.otherOp(4, not(13)) } returns 11

        every { mock.otherOp(5, or(or(more(20), 17), 13)) } returns 12

        val v = slot<Int>()
        every { mock.otherOp(6, and(capture(v), more(20))) } answers { v.captured }

        every { mock.otherOp(a = IntWrapper(7), b = isNull()) } returns 13
        every { mock.otherOp(a = IntWrapper(8), b = isNull(true)) } returns 14

        every { mock.otherOp(a = IntWrapper(9), b = ofType(IntWrapper::class)) } returns 15

        assertEquals(1, mock.otherOp(a, b))
        assertEquals(0, mock.otherOp(IntWrapper(3), IntWrapper(4)))
        assertEquals(1, mock.otherOp(IntWrapper(3), b))

        assertEquals(2, mock.otherOp(1, 1))
        assertEquals(3, mock.otherOp(1, 2))
        assertEquals(4, mock.otherOp(1, 3))

        assertEquals(5, mock.otherOp(2, 1))
        assertEquals(6, mock.otherOp(2, 2))
        assertEquals(7, mock.otherOp(2, 3))

        assertEquals(8, mock.otherOp(3, 3))
        assertEquals(0, mock.otherOp(3, 4))
        assertEquals(8, mock.otherOp(3, 5))
        assertEquals(0, mock.otherOp(3, 8))
        assertEquals(9, mock.otherOp(3, 9))
        assertEquals(9, mock.otherOp(3, 11))
        assertEquals(9, mock.otherOp(3, 14))
        assertEquals(0, mock.otherOp(3, 15))
        assertEquals(0, mock.otherOp(3, 19))
        assertEquals(10, mock.otherOp(3, 20))
        assertEquals(10, mock.otherOp(3, 100))

        assertEquals(11, mock.otherOp(4, 12))
        assertEquals(0, mock.otherOp(4, 13))
        assertEquals(11, mock.otherOp(4, 14))

        assertEquals(0, mock.otherOp(5, 12))
        assertEquals(12, mock.otherOp(5, 13))
        assertEquals(0, mock.otherOp(5, 14))
        assertEquals(0, mock.otherOp(5, 16))
        assertEquals(12, mock.otherOp(5, 17))
        assertEquals(0, mock.otherOp(5, 18))
        assertEquals(0, mock.otherOp(5, 20))
        assertEquals(12, mock.otherOp(5, 21))
        assertEquals(12, mock.otherOp(5, 100))

        assertEquals(0, mock.otherOp(6, 19))
        assertEquals(0, mock.otherOp(6, 20))
        assertEquals(21, mock.otherOp(6, 21))
        assertEquals(22, mock.otherOp(6, 22))
        assertEquals(23, mock.otherOp(6, 23))
        assertEquals(100, mock.otherOp(6, 100))

        assertEquals(13, mock.otherOp(IntWrapper(7), null))
        assertEquals(0, mock.otherOp(IntWrapper(7), IntWrapper(3)))

        assertEquals(0, mock.otherOp(IntWrapper(8), null))
        assertEquals(14, mock.otherOp(IntWrapper(8), IntWrapper(3)))

        assertEquals(15, mock.otherOp(IntWrapper(9), IntWrapper(3)))
        assertEquals(0, mock.otherOp(IntWrapper(9), DoubleWrapper(3.0)))

        verify {
            mock.otherOp(a, b)
            mock.otherOp(IntWrapper(3), IntWrapper(4))
            mock.otherOp(IntWrapper(3), b)

            mock.otherOp(1, 1)
            mock.otherOp(1, 2)
            mock.otherOp(1, 3)

            mock.otherOp(2, 1)
            mock.otherOp(2, 2)
            mock.otherOp(2, 3)

            mock.otherOp(3, or(3, 5))
        }
    }

    "nulls" {
        every { mock.otherOp(null, isNull()) } returns 4
        every { mock.nullableOp(1, 2) } returns null

        assertEquals(4, mock.otherOp(null, null))
        assertEquals(null, mock.nullableOp(1, 2))

        verify { mock.otherOp(isNull(), null) }
        verify { mock.nullableOp(1, 2) }
    }

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
        assertArrayEquals(
            Array<Double>(3, { (3 - it).toDouble() }),
            mock.arrayOp(Array<Double>(3, { (it + 1).toDouble() }))
        )

        assertArrayEquals(Array<Any>(3, { 3 - it }), mock.arrayOp(Array<Any>(3, { it + 1 })))
        assertArrayEquals(
            Array<Array<Any>>(3, { i -> Array<Any>(3, { j -> j - i }) }),
            mock.arrayOp(Array<Array<Any>>(3, { i -> Array<Any>(3, { j -> i + j }) }))
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
    }

    fun expectVerificationError(vararg messages: String, block: () -> Unit) {
        try {
            clearMocks(mock)
            block()
            fail("Block should throw verification failure")
        } catch (ex: AssertionError) {
            if (messages.any { !ex.message!!.contains(it) }) {
                fail("Bad message: " + ex.message)
            }
        }
    }

    "verification outcome" {
        expectVerificationError(
            "Only one matching call to ",
            "but arguments are not matching"
        ) {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)

            verify { mock.otherOp(1, 3) }
        }

        expectVerificationError("No matching calls found.", "Calls to same method") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 4)

            verify { mock.otherOp(1, 3) }
        }

        expectVerificationError("was not called", "Calls to same mock") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)

            verify { mock.manyArgsOp(true, false) }
        }

        expectVerificationError("was not called") {
            verify { mock.otherOp(1, 2) }
        }

        expectVerificationError("2 matching calls found, but needs at least 3 calls") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 2)

            verify(atLeast = 3) { mock.otherOp(1, 2) }
        }

        expectVerificationError("One matching call found, but needs at least 3 calls") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)

            verify(atLeast = 3) { mock.otherOp(1, 2) }
        }
        expectVerificationError("calls are not in verification order") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 3)

            verifyOrder {
                mock.otherOp(1, 3)
                mock.otherOp(1, 2)
            }
        }
        expectVerificationError("less calls happened then demanded by order verification sequence") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 3)

            verifyOrder {
                mock.otherOp(1, 3)
                mock.otherOp(1, 2)
            }
        }
        expectVerificationError("number of calls happened not matching exact number of verification sequence") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 3)

            verifySequence {
                mock.otherOp(1, 3)
                mock.otherOp(1, 2)
            }
        }
        expectVerificationError("calls are not exactly matching verification sequence") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 3)

            verifySequence {
                mock.otherOp(1, 3)
                mock.otherOp(1, 2)
            }
        }
        expectVerificationError("some calls were not matched") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 3)

            verifyAll {
                mock.otherOp(1, 2)
            }
        }
    }

    "lambda functions" {
        every {
            mock.lambdaOp(1, captureLambda())
        } answers { 1 - lambda<() -> Int>().invoke() }

        assertEquals(-4, mock.lambdaOp(1, { 5 }))

        verify {
            mock.lambdaOp(1, any())
        }
    }

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
        } answers {
            callOriginal()?.plus(4)
        }

        assertNotNull(spyObj.someReference)

        spyObj.doSomething()

        assertNull(spyObj.computeSomething(1))
        assertEquals(11, spyObj.computeSomething(2))
        assertEquals(8, spyObj.computeSomething(3))

        assertTrue(executed[0])
        assertTrue(executed[1])
        assertTrue(executed[2])
        assertTrue(executed[3])
    }
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

    fun opNeverCalled(): Int = 1
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


fun main(args: Array<String>) {
    MockKTestSuite()
}