package io.mockk.it

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.OptionalLong
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Test related to GitHub issue #973
 */
class OptionalReturnOnRelaxedMockTest {
    class ReturningOptionals {
        fun getOptional(): Optional<String> = Optional.empty()

        fun getOptionalInt(): OptionalInt = OptionalInt.empty()

        fun getOptionalLong(): OptionalLong = OptionalLong.empty()

        fun getOptionalDouble(): OptionalDouble = OptionalDouble.empty()
    }

    @RelaxedMockK
    private lateinit var returningOptionals: ReturningOptionals

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun getOptional() {
        val optional = returningOptionals.getOptional()
        assertFalse(optional.isPresent)
        assertEquals(Optional.empty<String>(), optional)
    }

    @Test
    fun getOptionalInt() {
        val optionalInt = returningOptionals.getOptionalInt()
        assertFalse(optionalInt.isPresent)
        assertEquals(OptionalInt.empty(), optionalInt)
    }

    @Test
    fun getOptionalLong() {
        val optionalLong = returningOptionals.getOptionalLong()
        assertFalse(optionalLong.isPresent)
        assertEquals(OptionalLong.empty(), optionalLong)
    }

    @Test
    fun getOptionalDouble() {
        val optionalDouble = returningOptionals.getOptionalDouble()
        assertFalse(optionalDouble.isPresent)
        assertEquals(OptionalDouble.empty(), optionalDouble)
    }
}
