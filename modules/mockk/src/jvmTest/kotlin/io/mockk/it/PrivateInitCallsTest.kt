package io.mockk.it

import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import java.util.TreeSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Issue #1288: calls made from field initializers and the Kotlin `init` block.
 *
 * `spyk(TestImpl(), recordPrivateCalls = true)` (copying an existing instance) can never see
 * these calls: the `init` block ran while the original object was constructed, before the spy
 * existed, so there is nothing to intercept. `spyk<TestImpl>()` on the other hand builds its
 * proxy by running the class's real default constructor, and since the init-block calls now
 * reach the spy they are recorded like any other call.
 */
class PrivateInitCallsTest {
    class TestImpl {
        var helloCalls = 0

        init {
            hello()
            greet()
        }

        fun initTest() {
            hello()
        }

        private fun hello() {
            helloCalls++
        }

        fun greet() {}
    }

    /** The call is made from a field initializer rather than the `init` block. */
    class FieldInit {
        var calls = 0

        val eager = first()

        private fun first(): String {
            calls++
            return "eager"
        }

        fun touch() {}
    }

    /**
     * The base constructor calls an open method while the proxy is still being built, so the
     * call dispatches to the subclass override and should reach the spy of the subclass.
     */
    open class HookInit {
        var hookedBy = "none"

        init {
            hook()
        }

        open fun hook() {
            hookedBy = "base"
        }
    }

    class OverridingHookInit : HookInit() {
        override fun hook() {
            hookedBy = "sub"
        }
    }

    class GreetInit {
        val fromCtor = greet()

        fun greet(): String = "ctor"
    }

    /**
     * `TreeSet.add` invokes `compareTo` from inside the JDK while the constructor window is
     * open. Those calls must run the original implementation and stay unrecorded, the same
     * way a static mock screens the JDK's own internal calls.
     */
    class SortedInit : Comparable<SortedInit> {
        var compares = 0

        init {
            TreeSet<SortedInit>().add(this)
        }

        override fun compareTo(other: SortedInit): Int {
            compares++
            return 0
        }

        fun touch() {}
    }

    @Test
    fun sanityPrivateCallAfterSpyCreation() {
        val spy = spyk(TestImpl(), recordPrivateCalls = true)
        spy.initTest()
        verify(exactly = 1) { spy["hello"]() }
        assertEquals(2, spy.helloCalls)
    }

    @Test
    fun classSpykRecordsInitBlockPrivateCall() {
        val spy = spyk<TestImpl>(recordPrivateCalls = true)
        verify(exactly = 1) { spy["hello"]() }
    }

    @Test
    fun classSpykRecordsInitBlockPublicCall() {
        val spy = spyk<TestImpl>(recordPrivateCalls = true)
        verify(exactly = 1) { spy.greet() }
    }

    @Test
    fun classSpykRecordsCtorTimeCallsInOrder() {
        val spy = spyk<TestImpl>(recordPrivateCalls = true)
        verifySequence {
            spy["hello"]()
            spy.greet()
        }
    }

    @Test
    fun classSpykRecordsFieldInitializerCall() {
        val spy = spyk<FieldInit>(recordPrivateCalls = true)
        assertEquals("eager", spy.eager)
        verify(exactly = 1) { spy["first"]() }
        assertEquals(1, spy.calls)
    }

    @Test
    fun classSpykOfSubclassRecordsBaseInitCallDispatchedToOverride() {
        val spy = spyk<OverridingHookInit>(recordPrivateCalls = true)
        assertEquals("sub", spy.hookedBy)
        verify(exactly = 1) { spy.hook() }
    }

    @Test
    fun classSpykWithoutRecordPrivateCallsRecordsOnlyPublicCtorTimeCall() {
        val spy = spyk<TestImpl>()
        assertEquals(1, spy.helloCalls)
        verify(exactly = 1) { spy.greet() }
    }

    @Test
    fun publicCtorTimeCallCanBeStubbedAfterCreation() {
        val spy = spyk<GreetInit>()
        assertEquals("ctor", spy.fromCtor)
        every { spy.greet() } returns "stubbed"
        assertEquals("stubbed", spy.greet())
        verify(exactly = 2) { spy.greet() }
    }

    @Test
    fun eachClassSpykRecordsItsOwnCtorTimeCalls() {
        val first = spyk<TestImpl>(recordPrivateCalls = true)
        val second = spyk<TestImpl>(recordPrivateCalls = true)
        verify(exactly = 1) { first["hello"]() }
        verify(exactly = 1) { first.greet() }
        verify(exactly = 1) { second["hello"]() }
        verify(exactly = 1) { second.greet() }
    }

    @Test
    fun jdkInternalCallDuringConstructionIsNotRecorded() {
        val spy = spyk<SortedInit>(recordPrivateCalls = true)
        assertTrue(spy.compares > 0, "TreeSet.add should have invoked the real compareTo from inside the JDK")
        verify(exactly = 0) { spy.compareTo(any()) }
    }
}
