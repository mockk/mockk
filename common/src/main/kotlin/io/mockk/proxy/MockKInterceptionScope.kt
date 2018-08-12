package io.mockk.proxy

interface MockKInterceptionScope {
    fun isInSafeScope(): Boolean

    fun enterSafeScope()

    fun exitSafeScope()
}

inline fun <T> MockKInterceptionScope.safeScope(on: Boolean, block: () -> T): T {
    val was = safeScope(on)
    return try {
        block()
    } finally {
        safeScope(was)
    }
}

inline fun MockKInterceptionScope.safeScope(on: Boolean): Boolean {
    val was = isInSafeScope()
    if (on) enterSafeScope() else exitSafeScope()
    return was
}
