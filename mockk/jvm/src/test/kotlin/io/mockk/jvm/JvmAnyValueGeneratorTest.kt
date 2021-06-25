package io.mockk.jvm

import io.mockk.impl.instantiation.JvmAnyValueGenerator
import io.mockk.util.assertArrayEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class JvmAnyValueGeneratorTest {

    private val generator = JvmAnyValueGenerator("")

    private val failOnPassThrough = { fail("Passed trough") }

    @Test
    fun givenByteClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0.toByte(), generator.anyValue(Byte::class, false, failOnPassThrough))
    }

    @Test
    fun givenShortClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0.toShort(), generator.anyValue(Short::class, false, failOnPassThrough))
    }

    @Test
    fun givenCharClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0.toChar(), generator.anyValue(Char::class, false, failOnPassThrough))
    }

    @Test
    fun givenIntClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0, generator.anyValue(Int::class, false, failOnPassThrough))
    }

    @Test
    fun givenLongClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0L, generator.anyValue(Long::class, false, failOnPassThrough))
    }

    @Test
    fun givenFloatClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0F, generator.anyValue(Float::class, false, failOnPassThrough))
    }

    @Test
    fun givenDoubleClassWhenRequestedForAnyValueThen0IsReturned() {
        assertEquals(0.0, generator.anyValue(Double::class, false, failOnPassThrough))
    }

    @Test
    fun givenStringClassWhenRequestedForAnyValueThenEmptyStringIsReturned() {
        assertEquals("", generator.anyValue(String::class, false, failOnPassThrough))
    }

    @Test
    fun givenBooleanArrayClassWhenRequestedForAnyValueThenEmptyBooleanArrayIsReturned() {
        assertArrayEquals(BooleanArray(0), generator.anyValue(BooleanArray::class, false, failOnPassThrough) as BooleanArray)
    }

    @Test
    fun givenByteArrayClassWhenRequestedForAnyValueThenEmptyByteArrayIsReturned() {
        assertArrayEquals(ByteArray(0), generator.anyValue(ByteArray::class, false, failOnPassThrough) as ByteArray)
    }

    @Test
    fun givenCharArrayClassWhenRequestedForAnyValueThenEmptyCharArrayIsReturned() {
        assertArrayEquals(CharArray(0), generator.anyValue(CharArray::class, false, failOnPassThrough) as CharArray)
    }

    @Test
    fun givenShortArrayClassWhenRequestedForAnyValueThenEmptyShortArrayIsReturned() {
        assertArrayEquals(ShortArray(0), generator.anyValue(ShortArray::class, false, failOnPassThrough) as ShortArray)
    }

    @Test
    fun givenIntArrayClassWhenRequestedForAnyValueThenEmptyIntArrayIsReturned() {
        assertArrayEquals(IntArray(0), generator.anyValue(IntArray::class, false, failOnPassThrough) as IntArray)
    }

    @Test
    fun givenLongArrayClassWhenRequestedForAnyValueThenEmptyLongArrayIsReturned() {
        assertArrayEquals(LongArray(0), generator.anyValue(LongArray::class, false, failOnPassThrough) as LongArray)
    }

    @Test
    fun givenFloatArrayClassWhenRequestedForAnyValueThenEmptyFloatArrayIsReturned() {
        assertArrayEquals(FloatArray(0), generator.anyValue(FloatArray::class, false, failOnPassThrough) as FloatArray, 1e-6f)
    }

    @Test
    fun givenDoubleArrayClassWhenRequestedForAnyValueThenEmptyDoubleArrayIsReturned() {
        assertArrayEquals(DoubleArray(0), generator.anyValue(DoubleArray::class, false, failOnPassThrough) as DoubleArray, 1e-6)
    }

    @Test
    fun givenListClassWhenRequestedForAnyValueThenEmptyListIsReturned() {
        assertEquals(listOf<Any>(), generator.anyValue(List::class, false, failOnPassThrough) as List<*>)
    }

    @Test
    fun givenMapClassWhenRequestedForAnyValueThenEmptyMapIsReturned() {
        assertEquals(mapOf<Any, Any>(), generator.anyValue(Map::class, false, failOnPassThrough) as Map<*, *>)
    }

    @Test
    fun givenSetClassWhenRequestedForAnyValueThenEmptySetIsReturned() {
        assertEquals(setOf<Any>(), generator.anyValue(Set::class, false, failOnPassThrough) as Set<*>)
    }

    @Test
    fun givenArrayListClassWhenRequestedForAnyValueThenEmptyArrayListIsReturned() {
        assertEquals(arrayListOf<Any>(), generator.anyValue(ArrayList::class, false, failOnPassThrough) as ArrayList<*>)
    }

    @Test
    fun givenHashMapClassWhenRequestedForAnyValueThenEmptyHashMapIsReturned() {
        assertEquals(hashMapOf<Any, Any>(), generator.anyValue(HashMap::class, false, failOnPassThrough) as HashMap<*, *>)
    }

    @Test
    fun givenHashSetClassWhenRequestedForAnyValueThenEmptyHashSetIsReturned() {
        assertEquals(hashSetOf<Any>(), generator.anyValue(HashSet::class, false, failOnPassThrough) as HashSet<*>)
    }
}
