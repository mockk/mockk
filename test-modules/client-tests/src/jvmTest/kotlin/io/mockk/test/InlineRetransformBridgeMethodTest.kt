package io.mockk.test

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Regression tests for issues #255, #477, #1118, #1487.
 *
 * When MockK uses Instrumentation.retransformClasses() via ByteBuddy, the JVM
 * prohibits adding or removing methods. ByteBuddy's redefine() can generate
 * bridge methods (visibility bridges, covariant return type bridges, generic
 * erasure bridges) that didn't exist in the original class, violating this
 * constraint and throwing UnsupportedOperationException.
 *
 * These tests exercise class hierarchies that would trigger bridge method
 * generation to ensure inline retransformation works correctly.
 */
class InlineRetransformBridgeMethodTest {

    // --- Covariant return type hierarchy ---

    abstract class BaseProducer {
        abstract fun produce(): Any
    }

    class StringProducer : BaseProducer() {
        override fun produce(): String = "original"
    }

    @Test
    fun `mock class with covariant return type`() {
        val mock = mockk<StringProducer>()
        every { mock.produce() } returns "mocked"
        assertEquals("mocked", mock.produce())
        verify { mock.produce() }
    }

    // --- Generic class hierarchy (erasure bridges) ---

    abstract class GenericBase<T> {
        abstract fun process(input: T): T
    }

    class StringProcessor : GenericBase<String>() {
        override fun process(input: String): String = input.uppercase()
    }

    @Test
    fun `mock class extending generic base`() {
        val mock = mockk<StringProcessor>()
        every { mock.process(any()) } returns "mocked"
        assertEquals("mocked", mock.process("input"))
        verify { mock.process("input") }
    }

    // --- Interface with default method + implementing class ---

    interface Greetable {
        fun greet(): String = "hello"
    }

    open class GreetableImpl : Greetable {
        override fun greet(): String = "hi"
    }

    @Test
    fun `mock class implementing interface with default method`() {
        val mock = mockk<GreetableImpl>()
        every { mock.greet() } returns "mocked"
        assertEquals("mocked", mock.greet())
        verify { mock.greet() }
    }

    // --- Multi-level covariant hierarchy ---

    interface Producer<out T> {
        fun get(): T
    }

    abstract class AbstractStringProducer : Producer<String>

    class ConcreteStringProducer : AbstractStringProducer() {
        override fun get(): String = "concrete"
    }

    @Test
    fun `mock class with multi-level covariant hierarchy`() {
        val mock = mockk<ConcreteStringProducer>()
        every { mock.get() } returns "mocked"
        assertEquals("mocked", mock.get())
        verify { mock.get() }
    }

    // --- Abstract class with suspend function ---

    abstract class AbstractService {
        abstract suspend fun fetch(): String
    }

    class ConcreteService : AbstractService() {
        override suspend fun fetch(): String = "data"
    }

    @Test
    fun `mock class extending abstract class with suspend function`() {
        val mock = mockk<ConcreteService>()
        every { mock.toString() } returns "ConcreteService-mock"
        assertEquals("ConcreteService-mock", mock.toString())
    }

    // --- Final class (inline-only mocking) with covariant parent ---

    open class OpenBaseWithReturn {
        open fun value(): Any = "base"
    }

    class FinalChildWithCovariantReturn : OpenBaseWithReturn() {
        override fun value(): String = "child"
    }

    @Test
    fun `mock final class with covariant return extending open class`() {
        val mock = mockk<FinalChildWithCovariantReturn>()
        every { mock.value() } returns "mocked"
        assertEquals("mocked", mock.value())
        verify { mock.value() }
    }
}
