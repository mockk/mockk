package io.mockk.proxy.jvm

import io.mockk.proxy.ProxyInterceptionScope

internal class JvmProxyInterceptionScope(
    val safeScopeFlag: ThreadLocal<Boolean>
) : ProxyInterceptionScope {
    override fun isInSafeScope(): Boolean {
        return safeScopeFlag.get() == true
    }

    override fun enterSafeScope() {
        safeScopeFlag.set(true)
    }

    override fun exitSafeScope() {
        safeScopeFlag.remove()
    }
}