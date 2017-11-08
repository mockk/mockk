package io.mockk.impl

import io.mockk.MockK
import io.mockk.MockKException
import io.mockk.MockKGateway.*
import io.mockk.external.logger
import javassist.util.proxy.ProxyObject
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

internal class MockFactoryImpl(val gateway: MockKGatewayImpl) : MockFactory {
    internal val log = logger<MockFactoryImpl>()

    override fun <T : Any> mockk(cls: KClass<T>, name: String?, moreInterfaces: Array<out KClass<*>>): T {
        val newName = name ?: "#${newId()}"
        log.debug { "Creating mockk for $cls name=$newName, moreInterfaces=${Arrays.toString(moreInterfaces)}" }
        val obj = gateway.instantiator.proxy(cls, false, moreInterfaces)
        if (obj !is MockK) {
            throw MockKException("Failed to create mock for $cls")
        }
        (obj as ProxyObject).handler = MockKInstanceProxyHandler(
                cls,
                newName,
                obj)
        return cls.cast(obj)
    }

    override fun <T : Any> spyk(cls: KClass<T>, objToCopy: T?, name: String?, moreInterfaces: Array<out KClass<*>>): T {
        val newName = name ?: "#${newId()}"
        log.debug { "Creating spyk for $cls name=$newName, moreInterfaces=${Arrays.toString(moreInterfaces)}" }
        val obj = gateway.instantiator.proxy(cls, objToCopy == null, moreInterfaces)
        if (obj !is MockK) {
            throw MockKException("Failed to create spy for $cls")
        }
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

    override fun clear(mocks: Array<out Any>, answers: Boolean, recordedCalls: Boolean, childMocks: Boolean) {
        for (mock in mocks) {
            if (mock !is MockKInstance) {
                throw MockKException("non-mock object is passed to clearMocks")
            }
            mock.___clear(answers, recordedCalls, childMocks)
        }
    }

    companion object {
        val idCounter = AtomicLong()

        fun newId(): Long = idCounter.incrementAndGet()
    }

}
