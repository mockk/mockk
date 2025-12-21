package io.mockk.impl.recording

import io.mockk.core.ValueClassSupport.boxedClass
import io.mockk.impl.instantiation.AbstractInstantiator
import io.mockk.impl.instantiation.AnyValueGenerator
import java.util.Random
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

class JvmSignatureValueGenerator(
    val rnd: Random,
) : SignatureValueGenerator {
    override fun <T : Any> signatureValue(
        cls: KClass<T>,
        anyValueGeneratorProvider: () -> AnyValueGenerator,
        instantiator: AbstractInstantiator,
    ): T {
        // For value classes, try the instance factory first.
        // If the factory provides an instance, use it.
        // Otherwise, fallback to the original behavior of constructing from the underlying boxed type.
        if (cls.isValue) {
            return instantiator.instantiateViaInstanceFactoryRegistry(cls) {
                // This lambda is the 'orInstantiate' part.
                // It's the original logic for handling value classes if no factory is found.
                val valueCls = cls.boxedClass
                val valueSig = signatureValue(valueCls, anyValueGeneratorProvider, instantiator) // Recursive call

                val constructor = cls.primaryConstructor!!.apply { isAccessible = true }
                constructor.call(valueSig)
            }
        }

        // Original logic for non-value classes
        return cls.cast(instantiate(cls, anyValueGeneratorProvider, instantiator))
    }

    private fun <T : Any> instantiate(
        cls: KClass<T>,
        anyValueGeneratorProvider: () -> AnyValueGenerator,
        instantiator: AbstractInstantiator,
    ): Any =
        when (cls) {
            Boolean::class -> rnd.nextBoolean()
            Byte::class -> rnd.nextInt().toByte()
            Short::class -> rnd.nextInt().toShort()
            Character::class -> rnd.nextInt().toChar()
            Integer::class -> rnd.nextInt()
            Long::class -> generateSafeLong()
            Float::class -> rnd.nextFloat()
            Double::class -> rnd.nextDouble()
            String::class -> rnd.nextLong().toString(16)

            else ->
                if (cls.isSealed) {
                    cls.sealedSubclasses.firstNotNullOfOrNull {
                        instantiate(it, anyValueGeneratorProvider, instantiator)
                    } ?: error("Unable to create proxy for sealed class $cls, available subclasses: ${cls.sealedSubclasses}")
                } else {
                    @Suppress("UNCHECKED_CAST")
                    anyValueGeneratorProvider().anyValue(cls, isNullable = false) {
                        instantiator.instantiate(cls)
                    } as T
                }
        }

    /**
     * Generates a Long value in Duration's safe millisecond range to avoid AssertionError.
     * Uses wider millisecond range while avoiding the denormalized range.
     * See: https://github.com/mockk/mockk/issues/1401
     */
    private fun generateSafeLong(): Long =
        if (rnd.nextBoolean()) {
            // Generate positive value in safe millisecond range
            nextLongInRange(MAX_NANOS_IN_MILLIS + 1, MAX_MILLIS)
        } else {
            // Generate negative value in safe millisecond range
            nextLongInRange(-MAX_MILLIS, -MAX_NANOS_IN_MILLIS)
        }

    /**
     * Generates a random Long value in the range [origin, bound).
     * This is a Java 8 compatible alternative to Random.nextLong(long, long) which was added in Java 17.
     *
     * Note: This implementation prioritizes simplicity and Java 8 compatibility over perfect uniformity.
     * For the purpose of signature value generation (avoiding denormalized Duration ranges),
     * this is sufficient.
     */
    private fun nextLongInRange(
        origin: Long,
        bound: Long,
    ): Long {
        require(origin < bound) { "origin ($origin) must be less than bound ($bound)" }
        val range = bound - origin
        // Use nextLong() and modulo to stay in range, then add origin
        // Math.floorMod handles negative values correctly
        return origin + Math.floorMod(rnd.nextLong(), range)
    }

    companion object {
        // Duration internal constants (mirrored from kotlin.time.Duration)
        // See: https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/src/kotlin/time/Duration.kt
        private const val NANOS_IN_MILLIS = 1_000_000L
        private const val MAX_NANOS = Long.MAX_VALUE / 2 / NANOS_IN_MILLIS * NANOS_IN_MILLIS - 1
        private const val MAX_MILLIS = Long.MAX_VALUE / 2
        internal const val MAX_NANOS_IN_MILLIS = MAX_NANOS / NANOS_IN_MILLIS
    }
}
