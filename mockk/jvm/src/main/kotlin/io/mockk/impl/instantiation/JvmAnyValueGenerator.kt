package io.mockk.impl.instantiation

import kotlin.reflect.KClass

class JvmAnyValueGenerator(instantiator: JvmInstantiator) : AnyValueGenerator() {
    val voidInstance = instantiator.instantiate(Void::class)

    override fun anyValue(cls: KClass<*>, orInstantiateVia: () -> Any?): Any? {
        return when (cls) {
            Void.TYPE.kotlin -> voidInstance
            Void::class -> voidInstance

            java.lang.Boolean::class -> false
            java.lang.Byte::class -> 0.toByte()
            java.lang.Short::class -> 0.toShort()
            java.lang.Character::class -> 0.toChar()
            java.lang.Integer::class -> 0
            java.lang.Long::class -> 0L
            java.lang.Float::class -> 0.0F
            java.lang.Double::class -> 0.0
            java.lang.Class::class -> Object::class.java

            else -> super.anyValue(cls) {
                if (cls.java.isArray) {
                    java.lang.reflect.Array.newInstance(cls.java.componentType, 0)
                } else {
                    orInstantiateVia()
                }
            }
        }
    }
}

