package io.mockk.impl.instantiation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class AnyValueGeneratorTest {
    val generator = AnyValueGenerator()

    val failOnPassThrough = { fail("Passed trough") }

    @Test
    fun givenByteClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0.toByte(), generator.anyValue(Byte::class, failOnPassThrough))
    }

    @Test
    fun givenShortClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0.toShort(), generator.anyValue(Short::class, failOnPassThrough))
    }

    @Test
    fun givenCharClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0.toChar(), generator.anyValue(Char::class, failOnPassThrough))
    }

    @Test
    fun givenIntClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0, generator.anyValue(Int::class, failOnPassThrough))
    }

    @Test
    fun givenLongClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0L, generator.anyValue(Long::class, failOnPassThrough))
    }

    @Test
    fun givenFloatClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0F, generator.anyValue(Float::class, failOnPassThrough))
    }

    @Test
    fun givenDoubleClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0.0, generator.anyValue(Double::class, failOnPassThrough))
    }

    @Test
    fun givenStringClassWhenRequestedForAnyValueThenEmptyStringIsReturned() {
        assertEquals("", generator.anyValue(String::class, failOnPassThrough))
    }

    @Test
    fun givenBooleanArrayClassWhenRequestedForAnyValueThenEmptyBooleanArrayIsReturned() {
        assertArrayEquals(BooleanArray(0), generator.anyValue(BooleanArray::class, failOnPassThrough) as BooleanArray)
    }

    @Test
    fun givenByteArrayClassWhenRequestedForAnyValueThenEmptyByteArrayIsReturned() {
        assertArrayEquals(ByteArray(0), generator.anyValue(ByteArray::class, failOnPassThrough) as ByteArray)
    }

    @Test
    fun givenCharArrayClassWhenRequestedForAnyValueThenEmptyCharArrayIsReturned() {
        assertArrayEquals(CharArray(0), generator.anyValue(CharArray::class, failOnPassThrough) as CharArray)
    }

    @Test
    fun givenShortArrayClassWhenRequestedForAnyValueThenEmptyShortArrayIsReturned() {
        assertArrayEquals(ShortArray(0), generator.anyValue(ShortArray::class, failOnPassThrough) as ShortArray)
    }

    @Test
    fun givenIntArrayClassWhenRequestedForAnyValueThenEmptyIntArrayIsReturned() {
        assertArrayEquals(IntArray(0), generator.anyValue(IntArray::class, failOnPassThrough) as IntArray)
    }

    @Test
    fun givenLongArrayClassWhenRequestedForAnyValueThenEmptyLongArrayIsReturned() {
        assertArrayEquals(LongArray(0), generator.anyValue(LongArray::class, failOnPassThrough) as LongArray)
    }

    @Test
    fun givenFloatArrayClassWhenRequestedForAnyValueThenEmptyFloatArrayIsReturned() {
        assertArrayEquals(FloatArray(0), generator.anyValue(FloatArray::class, failOnPassThrough) as FloatArray)
    }

    @Test
    fun givenDoubleArrayClassWhenRequestedForAnyValueThenEmptyDoubleArrayIsReturned() {
        assertArrayEquals(DoubleArray(0), generator.anyValue(DoubleArray::class, failOnPassThrough) as DoubleArray)
    }

    fun assertArrayEquals(actual: BooleanArray, expected: BooleanArray) {
        assertTrue(actual contentEquals expected)
    }

    fun assertArrayEquals(actual: ByteArray, expected: ByteArray) {
        assertTrue(actual contentEquals expected)
    }

    fun assertArrayEquals(actual: ShortArray, expected: ShortArray) {
        assertTrue(actual contentEquals expected)
    }

    fun assertArrayEquals(actual: CharArray, expected: CharArray) {
        assertTrue(actual contentEquals expected)
    }

    fun assertArrayEquals(actual: IntArray, expected: IntArray) {
        assertTrue(actual contentEquals expected)
    }

    fun assertArrayEquals(actual: LongArray, expected: LongArray) {
        assertTrue(actual contentEquals expected)
    }

    fun assertArrayEquals(actual: FloatArray, expected: FloatArray) {
        assertTrue(actual contentEquals expected)
    }

    fun assertArrayEquals(actual: DoubleArray, expected: DoubleArray) {
        assertTrue(actual contentEquals expected)
    }
}
