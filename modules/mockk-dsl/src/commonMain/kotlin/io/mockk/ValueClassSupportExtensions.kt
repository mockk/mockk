package io.mockk

import io.mockk.core.ValueClassSupport.boxedClass
import kotlin.reflect.KClass

internal fun KClass<*>.innermostBoxedClass(): KClass<*> = boxedClassChain().last()

internal fun KClass<*>.boxedClassChain(): List<KClass<*>> {
    val result = mutableListOf<KClass<*>>()
    val visited = mutableSetOf<KClass<*>>()
    var current = this

    while (visited.add(current)) {
        result += current
        val boxed = runCatching { current.boxedClass }.getOrElse { return result }
        if (boxed == current) return result
        current = boxed
    }

    return result
}

internal fun KClass<*>.valueClassAwareIsInstance(
    arg: Any?,
    parameterType: KClass<*>?,
): Boolean {
    if (arg == null) return false
    if (isInstance(arg)) return true

    return parameterType != null &&
        boxedClassChain()
            .drop(1)
            .any { boxedType -> boxedType == parameterType && boxedType.isInstance(arg) }
}
