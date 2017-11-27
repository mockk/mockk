package io.mockk.impl

import io.mockk.MockKException
import io.mockk.MockKGateway.*
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import io.mockk.InternalPlatform.toStr

internal class MockFactoryImpl(val gateway: MockKGatewayImpl) : MockFactory {
    override fun <T : Any> mockk(cls: KClass<T>, name: String?, moreInterfaces: Array<out KClass<*>>): T {
        val newName = name ?: "#${newId()}"
        log.debug { "Creating mockk for ${cls.toStr()} name=$newName, moreInterfaces=${Arrays.toString(moreInterfaces)}" }

        val stub = MockKStub(cls, newName)

        val obj = gateway.instantiator.proxy(cls,
                false,
                false,
                moreInterfaces,
                stub)

        gateway.stubs.put(obj, stub)

        return cls.cast(obj)
    }

    override fun <T : Any> spyk(cls: KClass<T>?, objToCopy: T?, name: String?, moreInterfaces: Array<out KClass<*>>): T {
        val newName = name ?: "#${newId()}"
        log.debug { "Creating spyk for ${cls.toStr()} name=$newName, moreInterfaces=${Arrays.toString(moreInterfaces)}" }

        val clazz = when {
            objToCopy != null -> objToCopy::class
            cls != null -> cls
            else -> throw MockKException("Either cls or objToCopy should not be null")
        }

        val stub = SpyKStub(clazz, newName)

        val obj = gateway.instantiator.proxy(clazz,
                objToCopy == null,
                false,
                moreInterfaces,
                stub)

        if (objToCopy != null) {
            copyFields(obj, objToCopy, objToCopy.javaClass)
        }

        gateway.stubs.put(obj, stub)

        return clazz.cast(obj)
    }

    private fun copyFields(obj: Any, objToCopy: Any, cls: Class<*>) {
        for (field in cls.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) {
                continue
            }
            field.isAccessible = true
            val value = field.get(objToCopy)
            field.set(obj, value)
            log.trace { "Copied field $field of $cls" }
        }
        if (cls.superclass != null) {
            copyFields(obj, objToCopy, cls.superclass)
        }
    }

    override fun clear(mocks: Array<out Any>, answers: Boolean, recordedCalls: Boolean, childMocks: Boolean) {
        for (mock in mocks) {
            gateway.stubFor(mock).clear(answers, recordedCalls, childMocks)
        }
    }

    override fun staticMockk(cls: KClass<*>) {
        log.debug { "Creating static mockk for ${cls.toStr()}" }

        val stub = MockKStub(cls, "static " + cls.simpleName)

        gateway.instantiator.staticMockk(cls, stub)

        gateway.stubs.put(cls.java, stub)
    }

    override fun staticUnMockk(cls: KClass<*>) {
        gateway.instantiator.staticUnMockk(cls)
    }

    companion object {
        val idCounter = AtomicLong()

        fun newId(): Long = idCounter.incrementAndGet()

        val log = logger<MockFactoryImpl>()
    }

}
