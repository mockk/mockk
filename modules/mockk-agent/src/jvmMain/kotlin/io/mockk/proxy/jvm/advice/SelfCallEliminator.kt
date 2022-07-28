package io.mockk.proxy.jvm.advice

import java.lang.reflect.Method
import java.util.*

internal object SelfCallEliminator {
    val selfCall = ThreadLocal<Any>()
    val selfCallMethod = ThreadLocal<Method>()

    fun isSelf(self: Any, method: Method): Boolean {
        return selfCall.get() === self && checkOverride(selfCallMethod.get(), method)
    }

    private fun checkOverride(method1: Method, method2: Method): Boolean {
        return method1.name == method2.name && Arrays.equals(method1.parameterTypes, method2.parameterTypes)
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
