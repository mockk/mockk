package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Calls that reach a mock through a compiler-generated bridge method.
 *
 * Overriding a member with a more specific type makes the compiler emit a synthetic bridge with
 * the erased signature next to the real method. For a `final` property with a backing field the
 * Kotlin compiler does not make that bridge delegate to the getter: it reads the field itself.
 * A call through the supertype therefore has to be intercepted at the bridge and recorded against
 * the method it bridges, otherwise it returns the field of the mock instead of the stubbed value.
 */
class BridgeMethodTest {
    interface NumberSource {
        val number: Number
    }

    abstract class IntSource(
        final override val number: Int,
    ) : NumberSource

    class FinalIntSource(
        number: Int,
    ) : IntSource(number)

    interface Source<T> {
        val value: T
    }

    open class StringSource(
        final override val value: String,
    ) : Source<String>

    interface Sink<T> {
        fun accept(item: T): Boolean
    }

    class StringSink : Sink<String> {
        override fun accept(item: String): Boolean = false
    }

    @Test
    fun stubbedCovariantFinalPropertyIsReturnedThroughSupertype() {
        val mock = mockk<FinalIntSource>()

        every { mock.number } returns 800

        assertEquals(800, mock.number)
        assertEquals(800, (mock as IntSource).number)
        assertEquals(800, (mock as NumberSource).number)
    }

    @Test
    fun callThroughSupertypeIsVerifiedAgainstTheOverridingProperty() {
        val mock = mockk<FinalIntSource>()
        every { mock.number } returns 800

        (mock as NumberSource).number

        verify(exactly = 1) { mock.number }
    }

    @Test
    fun stubRecordedThroughSupertypeAnswersTheOverridingProperty() {
        val mock = mockk<FinalIntSource>()

        every { (mock as NumberSource).number } returns 7

        assertEquals(7, mock.number)
        assertEquals(7, (mock as NumberSource).number)
    }

    @Test
    fun stubbedFinalPropertyOfGenericSupertypeIsReturnedThroughSupertype() {
        val mock = mockk<StringSource>()

        every { mock.value } returns "stubbed"

        assertEquals("stubbed", (mock as Source<*>).value)
    }

    @Test
    fun spyCallThroughSupertypeIsRecordedOnce() {
        val spy = spyk(FinalIntSource(5))

        assertEquals(5, (spy as NumberSource).number)

        verify(exactly = 1) { spy.number }
    }

    @Test
    fun bridgeKeepsItsOwnBehaviourForArgumentsTheBridgedMethodCannotTake() {
        val mock = mockk<StringSink>()
        every { mock.accept("stubbed") } returns true

        @Suppress("UNCHECKED_CAST")
        val untyped = mock as Sink<Any>

        assertTrue(untyped.accept("stubbed"))
        assertFailsWith<ClassCastException> { untyped.accept(1) }
    }
}
