package io.mockk.impl.recording

import io.mockk.InternalPlatformDsl
import io.mockk.impl.instantiation.AbstractInstantiator
import io.mockk.impl.instantiation.AnyValueGenerator
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
            val valueCls = InternalPlatformDsl.unboxClass(cls)
            val valueSig = signatureValue(valueCls, anyValueGeneratorProvider, instantiator)

            val constructor = cls.primaryConstructor!!.apply { isAccessible = true }
            return constructor.call(valueSig)
        }

        return cls.cast(
            when (cls) {
                java.lang.Boolean::class -> rnd.nextBoolean()
                java.lang.Byte::class -> rnd.nextInt().toByte()
                java.lang.Short::class -> rnd.nextInt().toShort()
                java.lang.Character::class -> rnd.nextInt().toChar()
                java.lang.Integer::class -> rnd.nextInt()
                java.lang.Long::class -> rnd.nextLong()
                java.lang.Float::class -> rnd.nextFloat()
                java.lang.Double::class -> rnd.nextDouble()
                java.lang.String::class -> rnd.nextLong().toString(16)

                else ->
                    @Suppress("UNCHECKED_CAST")
                    anyValueGeneratorProvider().anyValue(cls, isNullable = false) {
                        instantiator.instantiate(cls)
                    } as T
            }
        )
    }
}
