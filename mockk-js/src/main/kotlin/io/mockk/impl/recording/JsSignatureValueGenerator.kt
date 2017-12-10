package io.mockk.impl.recording

import kotlin.js.Math
import kotlin.reflect.KClass

class JsSignatureValueGenerator : SignatureValueGenerator {
    fun rnd(min: Number, max: Number): Double {
        return Math.random() * (max.toDouble() - min.toDouble()) + min.toDouble()
    }

    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
    override fun <T : Any> signatureValue(cls: KClass<T>, orInstantiateVia: () -> T): T {
        with(cls) {
            return when (cls) {
                Boolean::class -> rnd(0, 1) > 0.5
                Byte::class -> rnd(Byte.MIN_VALUE, Byte.MAX_VALUE).toByte()
                Short::class -> rnd(Short.MIN_VALUE, Short.MAX_VALUE).toShort()
                Char::class -> rnd(0, 65535).toInt()
                Int::class -> rnd(Int.MIN_VALUE, Int.MAX_VALUE).toInt()
                Long::class -> rnd(Long.MIN_VALUE, Long.MAX_VALUE).toLong()
                Float::class -> rnd(Float.MIN_VALUE, Float.MAX_VALUE).toFloat()
                Double::class -> rnd(Double.MIN_VALUE, Double.MAX_VALUE)
                String::class -> rnd(Long.MIN_VALUE, Long.MAX_VALUE).toString()
                else -> orInstantiateVia()
            } as T
        }
    }
}