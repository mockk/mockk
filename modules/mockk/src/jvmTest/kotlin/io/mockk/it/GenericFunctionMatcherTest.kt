package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.function.Function
import kotlin.test.Test
import kotlin.test.assertSame

class GenericFunctionMatcherTest {
    class Box<T>(
        val value: T,
    )

    interface Processor {
        fun <T> run(
            source: Box<T>,
            fallback: Function<Throwable, Box<T>>,
        ): Box<T>
    }

    private val processor = mockk<Processor>()
    private val source = Box("source")
    private val fallback = Function<Throwable, Box<String>> { Box("fallback") }

    @Test
    fun ofTypeInfersGenericFunctionFromTypedArgument() {
        // See #555: ofType(Box::class) loses the element type needed to infer the second matcher.
        every { processor.run(ofType<Box<String>>(), ofType()) } answers { firstArg() }

        assertSame(source, processor.run(source, fallback))

        verify(exactly = 1) { processor.run(ofType<Box<String>>(), ofType()) }
    }

    @Test
    fun ofTypeWithExplicitGenericFunctionType() {
        every { processor.run(ofType<Box<String>>(), ofType<Function<Throwable, Box<String>>>()) } answers { firstArg() }

        assertSame(source, processor.run(source, fallback))

        verify(exactly = 1) { processor.run(ofType<Box<String>>(), ofType<Function<Throwable, Box<String>>>()) }
    }

    @Test
    fun anyWithExplicitGenericFunctionType() {
        every { processor.run(any<Box<String>>(), any<Function<Throwable, Box<String>>>()) } answers { firstArg() }

        assertSame(source, processor.run(source, fallback))

        verify(exactly = 1) { processor.run(any<Box<String>>(), any<Function<Throwable, Box<String>>>()) }
    }
}
