package io.mockk.impl.recording

import io.mockk.impl.instantiation.AbstractInstantiator
import io.mockk.impl.instantiation.AnyValueGenerator
import io.mockk.impl.instantiation.CommonInstanceFactoryRegistry
import io.mockk.impl.recording.JvmSignatureValueGenerator.Companion.MAX_NANOS_IN_MILLIS
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.fail
import kotlin.time.Duration
import java.util.Random
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class JvmSignatureValueGeneratorTest {

    private val generator = JvmSignatureValueGenerator(Random(42))

    private val mockInstantiator = object : AbstractInstantiator(CommonInstanceFactoryRegistry()) {
        override fun <T : Any> instantiate(cls: KClass<T>): T = fail("Should not instantiate")
    }

    private val mockAnyValueGenerator = object : AnyValueGenerator() {
        override fun anyValue(
            cls: KClass<*>,
            isNullable: Boolean,
            orInstantiateVia: () -> Any?
        ): Any? = fail("Should not generate any value")
    }

    @Test
    fun `generated Long values avoid denormalized range`() {
        repeat(TEST_ITERATIONS) {
            val longValue = generator.signatureValue(
                Long::class,
                { mockAnyValueGenerator },
                mockInstantiator
            )

            assertTrue(
                longValue !in -MAX_NANOS_IN_MILLIS..MAX_NANOS_IN_MILLIS,
                "Generated Long value $longValue is in denormalized range"
            )
        }
    }

    @Test
    fun `Duration creation with generated Long values does not throw AssertionError`() {
        repeat(TEST_ITERATIONS) {
            val longValue = generator.signatureValue(
                Long::class,
                { mockAnyValueGenerator },
                mockInstantiator
            )

            try {
                longValue.toDuration(DurationUnit.MILLISECONDS)
            } catch (e: AssertionError) {
                fail("Duration creation with $longValue threw AssertionError: ${e.message}")
            }
        }
    }

    @Test
    fun `Duration value class creation succeeds`() {
        repeat(TEST_ITERATIONS) {
            val duration = generator.signatureValue(
                Duration::class,
                { mockAnyValueGenerator },
                mockInstantiator
            )

            assertNotNull(duration, "Duration should be created successfully")
        }
    }

    companion object {
        private const val TEST_ITERATIONS = 1000
    }
}
