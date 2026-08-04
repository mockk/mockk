package io.mockk.it

import io.mockk.MockKException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.lang.reflect.InvocationTargetException
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

// replica of issue #1439: kotlin-reflect fails with ClassNotFoundException: kotlin.Array
// when analyzing a companion containing a @JvmStatic external function returning Array
private class HasExternalJniMethod private constructor() {
    companion object {
        val instance: HasExternalJniMethod = HasExternalJniMethod()

        @Suppress("unused")
        @JvmStatic
        external fun jniMethod(): Array<String>
    }
}

class InlineMemberFunctionFailFastTest {
    @Test
    fun stubOfNonInlineWrapperReturnsConfiguredValue() {
        val w = mockk<HasWrapper>()
        every { w.addOne(10) } returns 42
        assertEquals(42, w.addOne(10))
    }

    @Test
    fun stubOfInlineFunctionFromKotlinThrowsDescriptiveError() {
        val inlineMock = mockk<HasInline>()
        val ex =
            assertFailsWith<MockKException> {
                every { inlineMock.addOne(any()) } returns 42
            }
        assertContains(ex.message!!, "Kotlin inline function")
    }

    @Test
    fun stubOfInlineFunctionViaReflectionThrowsFailFastError() {
        val m = mockk<HasInline>()
        val method = HasInline::class.java.getDeclaredMethod("addOne", Int::class.javaPrimitiveType)

        val ex =
            assertFailsWith<InvocationTargetException> {
                method.isAccessible = true
                method.invoke(m, 10)
            }
        val cause =
            (ex.targetException ?: ex.cause) as? MockKException
                ?: error("Unexpected cause: ${ex.targetException?.javaClass ?: ex.cause?.javaClass}")

        val msg = cause.message ?: ""
        assertContains(msg, "Mocking Kotlin inline functions is not supported")
    }

    /**
     * Inline detection must stay best-effort: when kotlin-reflect cannot analyze the declaring
     * class it throws (here: ClassNotFoundException for kotlin.Array, see #1439), and mocking
     * has to proceed as if the method were not inline instead of failing.
     */
    @Test
    fun stubOfCompanionWithExternalArrayReturningFunctionStillWorks() {
        mockkObject(HasExternalJniMethod.Companion)
        try {
            val replacement = mockk<HasExternalJniMethod>()
            every { HasExternalJniMethod.instance } returns replacement
            assertEquals(replacement, HasExternalJniMethod.instance)
        } finally {
            unmockkObject(HasExternalJniMethod.Companion)
        }
    }

    /**
     * Same best-effort requirement for a Java hierarchy kotlin-reflect rejects with
     * "Cannot infer visibility for inherited open fun clone" (see #1432).
     */
    @Test
    fun stubOfJavaClassWithCloneDeclaredOnSubInterfaceStillWorks() {
        val trigger = mockk<CloneDeclaredOnSubInterface.SimpleTriggerImpl>()
        every { trigger.timesTriggered } returns 1
        assertEquals(1, trigger.timesTriggered)
    }
}
