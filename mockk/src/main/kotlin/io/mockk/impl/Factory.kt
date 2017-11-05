package io.mockk.impl

import io.mockk.MockFactory
import io.mockk.external.logger
import javassist.util.proxy.ProxyObject
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

internal class MockFactoryImpl(val gw: MockKGatewayImpl) : MockFactory {
    internal val log = logger<MockFactoryImpl>()

    override fun <T> mockk(cls: Class<T>, name: String?, moreInterfaces: Array<out KClass<*>>): T {
        val newName = name ?: "#${newId()}"
        log.debug { "Creating mockk for $cls name=$newName, moreInterfaces=${Arrays.toString(moreInterfaces)}" }
        val obj = gw.instantiator.proxy(cls, false, moreInterfaces)
        (obj as ProxyObject).handler = MockKInstanceProxyHandler(
                cls,
                newName,
                obj)
        return cls.cast(obj)
    }

    override fun <T> spyk(cls: Class<T>, objToCopy: T?, name: String?, moreInterfaces: Array<out KClass<*>>): T {
        val newName = name ?: "#${newId()}"
        log.debug { "Creating spyk for $cls name=$newName, moreInterfaces=${Arrays.toString(moreInterfaces)}"  }
        val obj = gw.instantiator.proxy(cls, objToCopy == null, moreInterfaces)
        if (objToCopy != null) {
            copyFields(obj, objToCopy as Any)
        }
        (obj as ProxyObject).handler = SpyKInstanceProxyHandler(
                cls,
                newName,
                obj)
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
