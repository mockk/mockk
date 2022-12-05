package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.util.assertArrayEquals
import io.mockk.verify
import kotlin.test.Test

class ArraysTest {
    val mock = mockk<MockCls>()

    @Test
    fun booleanArray() {
        every { mock.arrayOp(BooleanArray(3) { true }) } returns BooleanArray(3) { false }

        assertArrayEquals(BooleanArray(3) { false }, mock.arrayOp(BooleanArray(3) { true }))

        verify { mock.arrayOp(BooleanArray(3) { true }) }
    }

    @Test
    fun byteArray() {
        every { mock.arrayOp(ByteArray(3) { (it + 1).toByte() }) } returns ByteArray(3) { (3 - it).toByte() }

        assertArrayEquals(ByteArray(3) { (3 - it).toByte() }, mock.arrayOp(ByteArray(3) { (it + 1).toByte() }))

        verify { mock.arrayOp(ByteArray(3) { (it + 1).toByte() }) }
    }

    @Test
    fun shortArray() {
        every { mock.arrayOp(ShortArray(3) { (it + 1).toShort() }) } returns ShortArray(3) { (3 - it).toShort() }

        assertArrayEquals(ShortArray(3) { (3 - it).toShort() }, mock.arrayOp(ShortArray(3) { (it + 1).toShort() }))

        verify { mock.arrayOp(ShortArray(3) { (it + 1).toShort() }) }
    }

    @Test
    fun charArray() {
        every { mock.arrayOp(CharArray(3) { (it + 1).toChar() }) } returns CharArray(3) { (3 - it).toChar() }

        assertArrayEquals(CharArray(3) { (3 - it).toChar() }, mock.arrayOp(CharArray(3) { (it + 1).toChar() }))

        verify { mock.arrayOp(CharArray(3) { (it + 1).toChar() }) }
    }

    @Test
    fun intArray() {
        every { mock.arrayOp(IntArray(3) { it + 1 }) } returns IntArray(3) { 3 - it }

        assertArrayEquals(IntArray(3) { 3 - it }, mock.arrayOp(IntArray(3) { it + 1 }))

        verify { mock.arrayOp(IntArray(3) { it + 1 }) }
    }

    @Test
    fun longArray() {
        every { mock.arrayOp(LongArray(3) { (it + 1).toLong() }) } returns LongArray(3) { (3 - it).toLong() }

        assertArrayEquals(LongArray(3) { (3 - it).toLong() }, mock.arrayOp(LongArray(3) { (it + 1).toLong() }))

        verify { mock.arrayOp(LongArray(3) { (it + 1).toLong() }) }
    }

    @Test
    fun floatArray() {
        every { mock.arrayOp(FloatArray(3) { (it + 1).toFloat() }) } returns FloatArray(3) { (3 - it).toFloat() }

        assertArrayEquals(
            FloatArray(3) { (3 - it).toFloat() },
            mock.arrayOp(FloatArray(3) { (it + 1).toFloat() }),
            1e-6f
        )

        verify { mock.arrayOp(FloatArray(3) { (it + 1).toFloat() }) }
    }

    @Test
    fun doubleArray() {
        every { mock.arrayOp(DoubleArray(3) { (it + 1).toDouble() }) } returns DoubleArray(3) { (3 - it).toDouble() }

        assertArrayEquals(
            DoubleArray(3) { (3 - it).toDouble() },
            mock.arrayOp(DoubleArray(3) { (it + 1).toDouble() }),
            1e-6
        )

        verify { mock.arrayOp(DoubleArray(3) { (it + 1).toDouble() }) }
    }

    @Test
    fun booleanObjectArray() {
        every { mock.arrayOp(Array(3) { true }) } returns Array(3) { false }

        assertArrayEquals(Array(3) { false }, mock.arrayOp(Array(3) { true }))

        verify { mock.arrayOp(Array(3) { true }) }
    }

    @Test
    fun byteObjectArray() {
        every { mock.arrayOp(Array(3) { (it + 1).toByte() }) } returns Array(3) { (3 - it).toByte() }

        assertArrayEquals(Array(3) { (3 - it).toByte() }, mock.arrayOp(Array(3) { (it + 1).toByte() }))

        verify { mock.arrayOp(Array(3) { (it + 1).toByte() }) }
    }

    @Test
    fun shortObjectArray() {
        every { mock.arrayOp(Array(3) { (it + 1).toShort() }) } returns Array(3) { (3 - it).toShort() }

        assertArrayEquals(
            Array(3) { (3 - it).toShort() },
            mock.arrayOp(Array(3) { (it + 1).toShort() })
        )

        verify { mock.arrayOp(Array(3) { (it + 1).toShort() }) }
    }

    @Test
    fun charObjectArray() {
        every { mock.arrayOp(Array(3) { (it + 1).toChar() }) } returns Array(3) { (3 - it).toChar() }

        assertArrayEquals(Array(3) { (3 - it).toChar() }, mock.arrayOp(Array(3) { (it + 1).toChar() }))

        verify { mock.arrayOp(Array(3) { (it + 1).toChar() }) }
    }

    @Test
    fun intObjectArray() {
        every { mock.arrayOp(Array(3) { it + 1 }) } returns Array(3) { 3 - it }

        assertArrayEquals(Array(3) { 3 - it }, mock.arrayOp(Array(3) { it + 1 }))

        verify { mock.arrayOp(Array(3) { it + 1 }) }
    }


    @Test
    fun longObjectArray() {
        every { mock.arrayOp(Array(3) { (it + 1).toLong() }) } returns Array(3) { (3 - it).toLong() }

        assertArrayEquals(Array(3) { (3 - it).toLong() }, mock.arrayOp(Array(3) { (it + 1).toLong() }))

        verify { mock.arrayOp(Array(3) { (it + 1).toLong() }) }
    }

    @Test
    fun floatObjectArray() {
        every { mock.arrayOp(Array(3) { (it + 1).toFloat() }) } returns Array(3) { (3 - it).toFloat() }

        assertArrayEquals(
            Array(3) { (3 - it).toFloat() },
            mock.arrayOp(Array(3) { (it + 1).toFloat() })
        )

        verify { mock.arrayOp(Array(3) { (it + 1).toFloat() }) }
    }

    @Test
    fun doubleObjectArray() {
        every {
            mock.arrayOp(Array(3) { (it + 1).toDouble() })
        } returns Array(3) { (3 - it).toDouble() }

        assertArrayEquals(
            Array(3) { (3 - it).toDouble() },
            mock.arrayOp(Array(3) { (it + 1).toDouble() })
        )

        verify { mock.arrayOp(Array(3) { (it + 1).toDouble() }) }
    }

    @Test
    fun anyAnyObjectArray() {
        every {
            mock.arrayOp(Array(3) { i -> Array(3) { j -> i + j } })
        } returns Array(3) { i -> Array(3) { j -> j - i } }

        assertArrayEquals(
            Array(3) { i -> Array<Any>(3) { j -> j - i } },
            mock.arrayOp(Array(3) { i -> Array(3) { j -> i + j } })
        )

        verify { mock.arrayOp(Array(3) { i -> Array(3) { j -> i + j } }) }
    }

    @Test
    fun intWrapperObjectArray() {
        every { mock.arrayOp(any<Array<IntWrapper>>()) } answers { Array(3) { IntWrapper(it + 2) } }

        assertArrayEquals(
            Array(3) { IntWrapper(it + 2) },
            mock.arrayOp(Array(3) { IntWrapper(it + 5) })
        )

        verify { mock.arrayOp(Array(3) { IntWrapper(it + 5) }) }
    }

    data class IntWrapper(val data: Int)

    class MockCls {
        fun arrayOp(arr: BooleanArray) = arr.map { it }.toBooleanArray()
        fun arrayOp(arr: ByteArray) = arr.map { (it + 1).toByte() }.toByteArray()
        fun arrayOp(arr: ShortArray) = arr.map { (it + 1).toShort() }.toShortArray()
        fun arrayOp(arr: CharArray) = arr.map { (it + 1) }.toCharArray()
        fun arrayOp(arr: IntArray) = arr.map { it + 1 }.toIntArray()
        fun arrayOp(arr: LongArray) = arr.map { it + 1 }.toLongArray()
        fun arrayOp(arr: FloatArray) = arr.map { it + 1 }.toFloatArray()
        fun arrayOp(arr: DoubleArray) = arr.map { it + 1 }.toDoubleArray()

        fun arrayOp(arr: Array<Boolean>) = arr.map { it }.toTypedArray()
        fun arrayOp(arr: Array<Byte>) = arr.map { (it + 1).toByte() }.toTypedArray()
        fun arrayOp(arr: Array<Short>) = arr.map { (it + 1).toShort() }.toTypedArray()
        fun arrayOp(arr: Array<Char>) = arr.map { it + 1 }.toTypedArray()
        fun arrayOp(arr: Array<Int>) = arr.map { it + 1 }.toTypedArray()
        fun arrayOp(arr: Array<Long>) = arr.map { it + 1 }.toTypedArray()
        fun arrayOp(arr: Array<Float>) = arr.map { it + 1 }.toTypedArray()
        fun arrayOp(arr: Array<Double>) = arr.map { it + 1 }.toTypedArray()

        fun arrayOp(array: Array<Array<Any>>): Array<Array<Any>> =
            array.map { outer -> outer.map { (it as Int) + 1 }.toTypedArray<Any>() }.toTypedArray()

        fun arrayOp(array: Array<IntWrapper>): Array<IntWrapper> = array.map { IntWrapper(it.data + 1) }.toTypedArray()
    }
}
