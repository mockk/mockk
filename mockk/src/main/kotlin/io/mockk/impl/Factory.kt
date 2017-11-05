package io.mockk.impl

import io.mockk.MockFactory
import io.mockk.external.logger
import javassist.util.proxy.ProxyObject
import java.util.concurrent.atomic.AtomicLong

internal class MockFactoryImpl(val gw: MockKGatewayImpl) : MockFactory {
    internal val log = logger<MockFactoryImpl>()

    override fun <T> mockk(cls: Class<T>): T {
        log.debug { "Creating mockk for $cls" }
        val obj = gw.instantiator.proxy(cls, false)
        (obj as ProxyObject).handler = MockKInstanceProxyHandler(cls, newId(), obj)
        return cls.cast(obj)
    }

    override fun <T> spyk(cls: Class<T>, objToCopy: T?): T {
        log.debug { "Creating spyk for $cls" }
        val obj = gw.instantiator.proxy(cls, objToCopy == null)
        if (objToCopy != null) {
            copyFields(obj, objToCopy as Any)
        }
        (obj as ProxyObject).handler = SpyKInstanceProxyHandler(cls, newId(), obj)
        return cls.cast(obj)
    }

    private fun copyFields(obj: Any, objToCopy: Any) {
        for (field in objToCopy.javaClass.declaredFields) {
            field.isAccessible = true
            field.set(obj, field.get(objToCopy))
            log.trace { "Copied field $field" }
        }
    }

    companion object {
        val idCounter = AtomicLong()

        fun newId(): Long = idCounter.incrementAndGet()
    }
}
