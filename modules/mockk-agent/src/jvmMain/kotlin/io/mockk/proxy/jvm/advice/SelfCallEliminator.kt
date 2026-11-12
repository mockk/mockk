package io.mockk.proxy.jvm.advice

import java.lang.reflect.Method

internal object SelfCallEliminator {
    val selfCall = ThreadLocal<Any>()
    val selfCallMethod = ThreadLocal<Method>()

    fun isSelf(
        self: Any,
        method: Method,
    ): Boolean = selfCall.get() === self && selfCallMethod.get().let { checkOverride(it, method) || it.bridges(method) }

    // A bridge running its own body forwards to the method it bridges, which may take a primitive
    // where the bridge takes an object, so checkOverride does not see it as the same call.
    private fun Method.bridges(method: Method): Boolean = isBridge && BridgeMethodResolver.findBridgedMethod(this) == method

    private fun checkOverride(
        method1: Method,
        method2: Method,
    ): Boolean {
        val namesMatch = method1.name == method2.name

        val parameterTypesMatch =
            method1.parameterTypes.contentEquals(method2.parameterTypes) ||
                (
                    method1.parameterTypes.size == method2.parameterTypes.size &&
                        method1.parameterTypes.zip(method2.parameterTypes).all { (type1, type2) ->
                            type1.isAssignableFrom(type2) || type2.isAssignableFrom(type1)
                        }
                )

        return namesMatch && parameterTypesMatch
    }

    inline fun <T> apply(
        self: Any,
        method: Method,
        block: () -> T,
    ): T {
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
