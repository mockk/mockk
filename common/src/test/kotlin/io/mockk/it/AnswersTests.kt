package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class AnswersTests {
    class MockCls {
        fun op(a: Int, b: Int, c: Int = 10, d: Int = 25) = a + b + c + d

        fun lambdaOp(a: Int, b: () -> Int) = a + b()
    }

    val spy = spyk(MockCls())

    @Test
    fun answerFirstArg() {
        every { spy.op(any(), 5) } answers { if (firstArg<Int>() == 1) 1 else 2 }

        assertEquals(1, spy.op(1, 5))
        assertEquals(2, spy.op(2, 5))
    }

    @Test
    fun answerSecondArg() {
        every { spy.op(6, any()) } answers { if (secondArg<Int>() == 2) 3 else 4 }

        assertEquals(3, spy.op(6, 2))
        assertEquals(4, spy.op(6, 3))
    }

    @Test
    fun answerThirdArg() {
        every { spy.op(any(), 7, any()) } answers { if (thirdArg<Int>() == 9) 5 else 6 }

        assertEquals(5, spy.op(2, 7, 9))
        assertEquals(6, spy.op(3, 7, 10))
    }

    @Test
    fun answerLastArg() {
        every { spy.op(any(), 7, d = any()) } answers { if (lastArg<Int>() == 11) 7 else 8 }

        assertEquals(7, spy.op(2, 7, d = 11))
        assertEquals(8, spy.op(3, 7, d = 12))
    }

    @Test
    fun answerNArgs() {
        every { spy.op(any(), 1) } answers { nArgs }

        assertEquals(4, spy.op(2, 1))
    }

    @Test
    fun answerParamTypesCount() {
        every { spy.op(any(), 2) } answers { method.paramTypes.size }

        assertEquals(4, spy.op(2, 2))
    }

    @Test
    fun answerCaptureList() {
        val lstNonNull = mutableListOf<Int>()
        every { spy.op(1, 2, d = capture(lstNonNull)) } answers { lstNonNull.captured() }

        assertEquals(22, spy.op(1, 2, d = 22))
    }

    @Test
    fun answerCaptureListNullable() {
        val lst = mutableListOf<Int?>()
        every { spy.op(3, 4, d = captureNullable(lst)) } answers { lst.captured()!! }

        assertEquals(33, spy.op(3, 4, d = 33))
    }

    @Test
    fun answerCaptureSlot() {
        val slot = slot<Int>()
        every { spy.op(3, 4, d = capture(slot)) } answers { slot.captured }

        assertEquals(44, spy.op(3, 4, d = 44))
    }

    @Test
    fun answerCaptureLambda() {
        val slot = slot<() -> Int>()
        every { spy.lambdaOp(1, capture(slot)) } answers { 2 + slot.invoke() }

        assertEquals(5, spy.lambdaOp(1, { 3 }))

        verify { spy.lambdaOp(1, assert { it.invoke() == 3 }) }
    }
}