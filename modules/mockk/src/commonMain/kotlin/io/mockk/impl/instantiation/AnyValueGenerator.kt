package io.mockk.impl.instantiation

import kotlin.reflect.KClass

open class AnyValueGenerator {
    open fun anyValue(cls: KClass<*>, isNullable: Boolean, orInstantiateVia: () -> Any?): Any? =
        when (cls) {
            Boolean::class -> false
            Byte::class -> 0.toByte()
            Short::class -> 0.toShort()
            Char::class -> 0.toChar()
            Int::class -> 0
            Long::class -> 0L
            Float::class -> 0.0F
            Double::class -> 0.0
            String::class -> ""

            BooleanArray::class -> BooleanArray(0)
            ByteArray::class -> ByteArray(0)
            CharArray::class -> CharArray(0)
            ShortArray::class -> ShortArray(0)
            IntArray::class -> IntArray(0)
            LongArray::class -> LongArray(0)
            FloatArray::class -> FloatArray(0)
            DoubleArray::class -> DoubleArray(0)

            List::class -> List<Any>(0) {}
            Map::class -> HashMap<Any, Any>()
            Set::class -> HashSet<Any>()
            ArrayList::class -> ArrayList<Any>()
            HashMap::class -> HashMap<Any, Any>()
            HashSet::class -> HashSet<Any>()

            else -> orInstantiateVia()
        }

}
