package io.mockk.impl.recording

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

class JvmSignatureValueGenerator(val rnd: Random) : SignatureValueGenerator {
    override fun <T : Any> signatureValue(cls: KClass<T>, orInstantiateVia: () -> T): T {
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
                else -> orInstantiateVia()
            }
        )
    }
}