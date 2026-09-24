package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Mocking members that are compiled to synthetic JVM methods.
 *
 * `@JvmSynthetic` (and its `@get:`/`@set:` targeted forms) sets `ACC_SYNTHETIC` on the
 * generated JVM method. Subclass proxies must still implement such methods when they are
 * abstract, otherwise invoking them on the mock fails with an `AbstractMethodError`.
 * Static ones have to be inlined like any other static method so `mockkStatic` can stub them,
 * while the compiler-generated static helpers such as `foo$default` stay untouched.
 */
class JvmSyntheticMemberTest {
    interface SyntheticPropertyInterface {
        @get:JvmSynthetic
        val value: String

        @get:JvmSynthetic
        @set:JvmSynthetic
        var mutableValue: Int
    }

    interface SyntheticFunctionInterface {
        @JvmSynthetic
        fun compute(arg: String): String
    }

    object SyntheticStaticHolder {
        @JvmStatic
        @JvmSynthetic
        fun compute(arg: String): String = "real-$arg"
    }

    abstract class SyntheticPropertyClass {
        @get:JvmSynthetic
        abstract val value: String
    }

    @Test
    fun syntheticInterfacePropertyGetterIsStubbed() {
        val mock = mockk<SyntheticPropertyInterface>()

        every { mock.value } returns "stubbed"

        assertEquals("stubbed", mock.value)
        verify { mock.value }
    }

    @Test
    fun syntheticInterfacePropertySetterIsStubbed() {
        val mock = mockk<SyntheticPropertyInterface>(relaxUnitFun = true)

        every { mock.mutableValue } returns 42

        assertEquals(42, mock.mutableValue)

        mock.mutableValue = 7

        verify { mock.mutableValue = 7 }
    }

    @Test
    fun syntheticInterfaceFunctionIsStubbed() {
        val mock = mockk<SyntheticFunctionInterface>()

        every { mock.compute("in") } returns "out"

        assertEquals("out", mock.compute("in"))
        verify { mock.compute("in") }
    }

    @Test
    fun syntheticAbstractClassPropertyGetterIsStubbed() {
        val mock = mockk<SyntheticPropertyClass>()

        every { mock.value } returns "stubbed"

        assertEquals("stubbed", mock.value)
    }

    @Test
    fun syntheticTopLevelFunctionIsStubbed() {
        mockkStatic(::syntheticTopLevelFunction)
        try {
            every { syntheticTopLevelFunction("in") } returns "out"

            assertEquals("out", syntheticTopLevelFunction("in"))
            verify { syntheticTopLevelFunction("in") }
        } finally {
            unmockkStatic(::syntheticTopLevelFunction)
        }
    }

    @Test
    fun syntheticJvmStaticFunctionIsStubbed() {
        mockkStatic(SyntheticStaticHolder::class)
        try {
            every { SyntheticStaticHolder.compute("in") } returns "out"

            assertEquals("out", SyntheticStaticHolder.compute("in"))
            verify { SyntheticStaticHolder.compute("in") }
        } finally {
            unmockkStatic(SyntheticStaticHolder::class)
        }
    }

    @Test
    fun defaultArgumentsBridgeIsNotRecordedInsteadOfTheFunction() {
        mockkStatic(::functionWithDefaultArgument)
        try {
            every { functionWithDefaultArgument() } returns 99

            assertEquals(99, functionWithDefaultArgument(5))
            assertEquals(70, functionWithDefaultArgument(7))
        } finally {
            unmockkStatic(::functionWithDefaultArgument)
        }
    }
}

@JvmSynthetic
fun syntheticTopLevelFunction(arg: String): String = "real-$arg"

fun functionWithDefaultArgument(a: Int = 5): Int = a * 10
