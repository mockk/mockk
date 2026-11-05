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
        // See #555: Box::class supplies Box<*>, which prevents inference of the second matcher here.
        every { processor.run(ofType<Box<String>>(), ofType()) } answers { firstArg() }

        assertSame(source, processor.run(source, fallback))

        verify(exactly = 1) { processor.run(ofType<Box<String>>(), ofType()) }
    }

    @Test
    fun anyInfersGenericFunctionFromTypedArgument() {
        every { processor.run(any<Box<String>>(), any()) } answers { firstArg() }

        assertSame(source, processor.run(source, fallback))

        verify(exactly = 1) { processor.run(any<Box<String>>(), any()) }
    }
}
