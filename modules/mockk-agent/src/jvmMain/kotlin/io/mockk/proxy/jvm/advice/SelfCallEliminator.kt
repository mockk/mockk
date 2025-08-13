package io.mockk.proxy.jvm.advice

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.Arrays

internal object SelfCallEliminator {
    val selfCall = ThreadLocal<Any>()
    val selfCallMethod = ThreadLocal<Method>()

    fun isSelf(self: Any, method: Method): Boolean {
        return selfCall.get() === self && checkOverride(selfCallMethod.get(), method)
    }

    private fun checkOverride(method1: Method, method2: Method): Boolean {
        val namesMatch = method1.name == method2.name

        val parameterTypesMatch = method1.parameterTypes.contentEquals(method2.parameterTypes) ||
            (method1.parameterTypes.size == method2.parameterTypes.size &&
                method1.parameterTypes.zip(method2.parameterTypes).all { (type1, type2) ->
                    type1.isAssignableFrom(type2) || type2.isAssignableFrom(type1)
                })

        return namesMatch && parameterTypesMatch
    }

    inline fun <T> apply(self: Any, method: Method, block: () -> T): T {
        val prevSelf = selfCall.get()
        val prevMethod = selfCallMethod.get()
        selfCall.set(self)
        selfCallMethod.set(method)

        return try {
            block()
        } finally {
            selfCall.set(prevSelf)
            selfCallMethod.set(prevMethod)
        }
    }
}
