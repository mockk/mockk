package io.mockk.it

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * A `finalize()` declared by an interface has to be mockable like any other function (#1212).
 *
 * The subclass proxy is built from a method graph that merges methods by name and parameter types,
 * ignoring the return type. Without the graph-level filter of Object-declared methods, an
 * interface-declared `finalize()` - whatever its return type - merges into the node of the concrete
 * `finalize()` declared by `java.lang.Object`, which is excluded from the generated subclass. The
 * proxy then either fails with an [AbstractMethodError] (`finalize(): Boolean`) or the call falls
 * through to the `protected` `Object.finalize` and fails with an [IllegalAccessError]
 * (`finalize(): Unit`), while `finalize(value: Int)` with its own signature keeps working.
 *
 * The class-level `finalize()` is genuinely the GC-related one and must stay untouched: a subclass
 * proxy keeps inheriting it instead of overriding it. Kotlin emits a declared `finalize()` with
 * `ACC_FINAL`, so Byte Buddy cannot override it in the generated subclass either way - the test
 * below pins that the proxy class really does not declare it, guarding the graph-level filter
 * against growing to class-declared methods.
 */
class InterfaceFinalizeTest {
    interface VoidFinalizeInterface {
        fun doSomething()

        fun finalize()
    }

    interface BooleanFinalizeInterface {
        fun doSomething()

        fun finalize(): Boolean
    }

    interface OneArgFinalizeInterface {
        fun doSomething()

        fun finalize(value: Int): Boolean
    }

    abstract class KotlinAbstractClassWithFinalize {
        abstract fun compute(): Int

        @Suppress("unused")
        protected fun finalize(): Unit = throw AssertionError("class-level finalize must never be invoked through the mock")
    }

    @Test
    fun interfaceVoidFinalizeIsStubbed() {
        val mock = mockk<VoidFinalizeInterface>()

        every { mock.finalize() } just Runs
        mock.finalize()

        verify { mock.finalize() }
    }

    @Test
    fun interfaceBooleanFinalizeIsStubbed() {
        val mock = mockk<BooleanFinalizeInterface>()

        every { mock.finalize() } returns true

        assertEquals(true, mock.finalize())
        verify { mock.finalize() }
    }

    @Test
    fun interfaceOneArgFinalizeIsStubbed() {
        val mock = mockk<OneArgFinalizeInterface>()

        every { mock.finalize(5) } returns true

        assertEquals(true, mock.finalize(5))
        verify { mock.finalize(5) }
    }

    @Test
    fun relaxedInterfaceFinalizeIsAnswered() {
        val mock = mockk<VoidFinalizeInterface>(relaxed = true)

        mock.finalize()

        verify { mock.finalize() }
    }

    @Test
    fun subclassProxyDoesNotOverrideClassLevelFinalize() {
        val mock = mockk<KotlinAbstractClassWithFinalize>()

        every { mock.compute() } returns 42
        assertEquals(42, mock.compute())

        // The subclass proxy inherits the class-level finalize instead of overriding it, so the
        // lookup against the proxy class itself has to fail. getDeclaredMethod looks only at the
        // class it is called on, not at its superclasses.
        assertFailsWith<NoSuchMethodException> {
            mock.javaClass.getDeclaredMethod("finalize")
        }
    }
}
