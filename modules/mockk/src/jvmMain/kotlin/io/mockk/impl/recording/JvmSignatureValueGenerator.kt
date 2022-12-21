package io.mockk.impl.recording

import io.mockk.impl.instantiation.AbstractInstantiator
import io.mockk.impl.instantiation.AnyValueGenerator
import io.mockk.core.ValueClassSupport.boxedClass
import java.util.Random
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

class JvmSignatureValueGenerator(val rnd: Random) : SignatureValueGenerator {
    override fun <T : Any> signatureValue(
        cls: KClass<T>,
        anyValueGeneratorProvider: () -> AnyValueGenerator,
        instantiator: AbstractInstantiator,
    ): T {

        if (cls.isValue) {
            val valueCls = cls.boxedClass
            val valueSig = signatureValue(valueCls, anyValueGeneratorProvider, instantiator)

            val constructor = cls.primaryConstructor!!.apply { isAccessible = true }
            return constructor.call(valueSig)
        }

        return cls.cast(instantiate(cls, anyValueGeneratorProvider, instantiator))
    }

    private fun <T : Any> instantiate(
        cls: KClass<T>,
        anyValueGeneratorProvider: () -> AnyValueGenerator,
        instantiator: AbstractInstantiator
    ): Any = when (cls) {
        Boolean::class -> rnd.nextBoolean()
        Byte::class -> rnd.nextInt().toByte()
        Short::class -> rnd.nextInt().toShort()
        Character::class -> rnd.nextInt().toChar()
        Integer::class -> rnd.nextInt()
        Long::class -> rnd.nextLong()
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
}
