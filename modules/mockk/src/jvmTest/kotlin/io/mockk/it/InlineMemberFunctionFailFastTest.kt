package io.mockk.it

import io.mockk.MockKException
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class HasInline {
    inline fun addOne(x: Int) = x + 1
}

private class HasWrapper {
    private val impl = HasInline()
    fun addOne(x: Int) = impl.addOne(x) // non-inline wrapper
}

class InlineMemberFunctionFailFastTest {

    @Test
    fun givenNonInlineWrapper_whenStubbed_returnsConfiguredValue() {
        val w = mockk<HasWrapper>()
        every { w.addOne(10) } returns 42
        assertEquals(42, w.addOne(10))
    }

    @Test
    fun givenInlineFunction_whenStubbedFromKotlin_throwsDescriptiveError() {
        val inlineMock = mockk<HasInline>()
        val ex = assertFailsWith<MockKException> {
            every { inlineMock.addOne(any()) } returns 42
        }
        assertContains(ex.message!!, "Kotlin inline function")
    }

    @Test
    fun givenInlineFunction_whenInvokedViaReflection_throwsFailFastError() {
        val m = mockk<HasInline>()
        val method = HasInline::class.java.getDeclaredMethod("addOne", Int::class.javaPrimitiveType)

        val ex = assertFailsWith<InvocationTargetException> {
            method.isAccessible = true
            method.invoke(m, 10)
        }
        val cause = (ex.targetException ?: ex.cause) as? MockKException
            ?: error("Unexpected cause: ${ex.targetException?.javaClass ?: ex.cause?.javaClass}")

        val msg = cause.message ?: ""
        assertContains(msg, "Mocking Kotlin inline functions is not supported")
    }

}
