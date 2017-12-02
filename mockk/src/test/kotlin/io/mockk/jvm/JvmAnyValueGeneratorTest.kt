package io.mockk.jvm

import org.junit.Assert.*
import org.junit.Test

class JvmAnyValueGeneratorTest {
    val generator = JvmAnyValueGenerator()

    val failOnPassThrough = { fail("Passed trough") }

    @Test
    fun `given Void class when requested for any value then Unit is returned`() {
        assertEquals(Unit, generator.anyValue(Void.TYPE.kotlin, failOnPassThrough))
    }

    @Test
    fun `given Byte class when requested for any value then 0 is returned`() {
        assertEquals(0.toByte(), generator.anyValue(Byte::class, failOnPassThrough))
    }

    @Test
    fun `given Short class when requested for any value then 0 is returned`() {
        assertEquals(0.toShort(), generator.anyValue(Short::class, failOnPassThrough))
    }

    @Test
    fun `given Char class when requested for any value then 0 is returned`() {
        assertEquals(0.toChar(), generator.anyValue(Char::class, failOnPassThrough))
    }

    @Test
    fun `given Int class when requested for any value then 0 is returned`() {
        assertEquals(0, generator.anyValue(Int::class, failOnPassThrough))
    }

    @Test
    fun `given Long class when requested for any value then 0 is returned`() {
        assertEquals(0L, generator.anyValue(Long::class, failOnPassThrough))
    }

    @Test
    fun `given Float class when requested for any value then 0 is returned`() {
        assertEquals(0F, generator.anyValue(Float::class, failOnPassThrough))
    }

    @Test
    fun `given Double class when requested for any value then 0 is returned`() {
        assertEquals(0.0, generator.anyValue(Double::class, failOnPassThrough))
    }

    @Test
    fun `given String class when requested for any value then empty string is returned`() {
        assertEquals("", generator.anyValue(String::class, failOnPassThrough))
    }

    // TODO more tests
}