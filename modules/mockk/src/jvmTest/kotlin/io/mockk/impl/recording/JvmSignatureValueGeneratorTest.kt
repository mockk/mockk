package io.mockk.impl.recording

import io.mockk.impl.instantiation.AbstractInstantiator
import io.mockk.impl.instantiation.AnyValueGenerator
import io.mockk.impl.instantiation.CommonInstanceFactoryRegistry
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
        // Calculate denormalized range boundaries (same as implementation)
        val nanosInMillis = 1_000_000L
        val maxNanos = Long.MAX_VALUE / 2 / nanosInMillis * nanosInMillis - 1
        val maxNanosInMillis = maxNanos / nanosInMillis

        repeat(10) {
            val longValue = generator.signatureValue(
                Long::class,
                { mockAnyValueGenerator },
                mockInstantiator
            )

            assertTrue(
                longValue !in -maxNanosInMillis..maxNanosInMillis,
                "Generated Long value $longValue is in denormalized range"
            )
        }
    }

    @Test
    fun `Duration creation with generated Long values does not throw AssertionError`() {
        repeat(10) {
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
        repeat(10) {
            val duration = generator.signatureValue(
                Duration::class,
                { mockAnyValueGenerator },
                mockInstantiator
            )

            assertNotNull(duration, "Duration should be created successfully")
        }
    }
}
