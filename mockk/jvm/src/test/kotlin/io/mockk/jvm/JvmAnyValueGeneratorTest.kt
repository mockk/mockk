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
        assertArrayEquals(FloatArray(0), generator.anyValue(FloatArray::class, failOnPassThrough) as FloatArray, 1e-6f)
    }

    @Test
    fun givenDoubleArrayClassWhenRequestedForAnyValueThenEmptyDoubleArrayIsReturned() {
        assertArrayEquals(DoubleArray(0), generator.anyValue(DoubleArray::class, failOnPassThrough) as DoubleArray, 1e-6)
    }

    @Test
    fun givenListClassWhenRequestedForAnyValueThenEmptyListIsReturned() {
        assertEquals(listOf<Any>(), generator.anyValue(List::class, failOnPassThrough) as List<*>)
    }

    @Test
    fun givenMapClassWhenRequestedForAnyValueThenEmptyMapIsReturned() {
        assertEquals(mapOf<Any, Any>(), generator.anyValue(Map::class, failOnPassThrough) as Map<*, *>)
    }

    @Test
    fun givenSetClassWhenRequestedForAnyValueThenEmptySetIsReturned() {
        assertEquals(setOf<Any>(), generator.anyValue(Set::class, failOnPassThrough) as Set<*>)
    }

    @Test
    fun givenArrayListClassWhenRequestedForAnyValueThenEmptyArrayListIsReturned() {
        assertEquals(arrayListOf<Any>(), generator.anyValue(ArrayList::class, failOnPassThrough) as ArrayList<*>)
    }

    @Test
    fun givenHashMapClassWhenRequestedForAnyValueThenEmptyHashMapIsReturned() {
        assertEquals(hashMapOf<Any, Any>(), generator.anyValue(HashMap::class, failOnPassThrough) as HashMap<*, *>)
    }

    @Test
    fun givenHashSetClassWhenRequestedForAnyValueThenEmptyHashSetIsReturned() {
        assertEquals(hashSetOf<Any>(), generator.anyValue(HashSet::class, failOnPassThrough) as HashSet<*>)
    }
}
