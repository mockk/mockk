package io.mockk.proxy

import java.util.concurrent.Callable

interface ProxyInterceptionScope {
    fun isInSafeScope(): Boolean

    fun enterSafeScope()

    fun exitSafeScope()
}

inline fun <T> ProxyInterceptionScope.safeScope(on: Boolean, callable: Callable<T>): Callable<T> {
    return Callable {
        safeScope(on) {
            callable.call()
        }
    }
}

inline fun <T> ProxyInterceptionScope.safeScope(on: Boolean, block: () -> T): T {
    val was = safeScope(on)
    return try {
        block()
    } finally {
        safeScope(was)
    }
}

inline fun ProxyInterceptionScope.safeScope(on: Boolean): Boolean {
    val was = isInSafeScope()
    if (on) enterSafeScope() else exitSafeScope()
    return was
}
