package io.mockk.impl.recording

import kotlin.reflect.KClass

class ChildHinter {
    private var childTypes = mutableMapOf<Int, KClass<*>>()

    fun nextChildType(defaultReturnType: () -> KClass<*>): KClass<*> {
        val type = childTypes[1]
        shift()
        return type ?: defaultReturnType()
    }

    private fun shift() {
        childTypes = childTypes
            .mapKeys { (k, _) -> k - 1 }
            .filter { (k, _) -> k > 0 }
            .toMutableMap()
    }

    fun hint(n: Int, cls: KClass<*>) {
        childTypes[n] = cls
    }
}