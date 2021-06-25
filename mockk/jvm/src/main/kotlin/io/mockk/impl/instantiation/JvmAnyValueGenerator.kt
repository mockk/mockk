package io.mockk.impl.instantiation

import kotlin.reflect.KClass

open class JvmAnyValueGenerator(
    private val voidInstance: Any
) : AnyValueGenerator() {

    override fun anyValue(cls: KClass<*>, isNullable: Boolean, orInstantiateVia: () -> Any?): Any? {
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

            java.util.List::class -> List<Any>(0) {}
            java.util.Map::class -> HashMap<Any, Any>()
            java.util.Set::class -> HashSet<Any>()
            java.util.ArrayList::class -> ArrayList<Any>()
            java.util.HashMap::class -> HashMap<Any, Any>()
            java.util.HashSet::class -> HashSet<Any>()

            else -> super.anyValue(cls, isNullable) {
                if (cls.java.isArray) {
                    java.lang.reflect.Array.newInstance(cls.java.componentType, 0)
                } else {
                    orInstantiateVia()
                }
            }
        }
    }
}

