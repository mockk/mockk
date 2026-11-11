package io.mockk.it

import io.mockk.MockKException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * Test related to GitHub issue #1605
 */
@Suppress("UNUSED_PARAMETER")
class KotlinClassParamTest {
    class MockCls {
        fun op(klass: KClass<*>): Boolean = true

        fun op(
            first: KClass<*>,
            second: KClass<*>,
        ): Boolean = true
    }

    @Test
    fun matchingAnyKClass() {
        val mock = mockk<MockCls>()
        every { mock.op(any()) } returns false
        assertFalse(mock.op(String::class))
        verify { mock.op(any()) }
    }

    @Test
    fun matchingAnyKClassInSeveralParameters() {
        val mock = mockk<MockCls>()
        every { mock.op(any(), any()) } returns false
        assertFalse(mock.op(String::class, Int::class))
        verify { mock.op(any(), any()) }
    }

    @Test
    fun matchingEqKClass() {
        val mock = mockk<MockCls>()
        every { mock.op(eq(String::class)) } returns false
        assertFalse(mock.op(String::class))
        verify { mock.op(eq(String::class)) }
    }

    @Test
    fun eqKClassMatchersStayPositional() {
        val mock = mockk<MockCls>()
        every { mock.op(eq(String::class), eq(Int::class)) } returns false
        assertFalse(mock.op(String::class, Int::class))
        // Signature placeholders must stay distinct per matcher, otherwise the reversed
        // arguments would match the same stub.
        assertFailsWith<MockKException> {
            mock.op(Int::class, String::class)
        }
    }

    @Test
    fun matchingMixedKClassMatcherAndLiteral() {
        val mock = mockk<MockCls>()
        every { mock.op(eq(String::class), any()) } returns false
        assertFalse(mock.op(String::class, Int::class))
        verify { mock.op(eq(String::class), any()) }
    }
}
