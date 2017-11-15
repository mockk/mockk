package io.mockk.impl

import io.mockk.MockKGateway.*
import io.mockk.external.logger
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

internal class MockFactoryImpl(val gateway: MockKGatewayImpl) : MockFactory {
    internal val log = logger<MockFactoryImpl>()

    override fun <T : Any> mockk(cls: KClass<T>, name: String?, moreInterfaces: Array<out KClass<*>>): T {
        val newName = name ?: "#${newId()}"
        log.debug { "Creating mockk for ${cls.toStr()} name=$newName, moreInterfaces=${Arrays.toString(moreInterfaces)}" }

        val stub = MockKStub(cls, newName)

        val obj = gateway.instantiator.proxy(cls,
                false,
                moreInterfaces,
                stub)

        gateway.stubs.put(obj, stub)

        return cls.cast(obj)
    }

    override fun <T : Any> spyk(cls: KClass<T>, objToCopy: T?, name: String?, moreInterfaces: Array<out KClass<*>>): T {
        val newName = name ?: "#${newId()}"
        log.debug { "Creating spyk for ${cls.toStr()} name=$newName, moreInterfaces=${Arrays.toString(moreInterfaces)}" }

        val stub = SpyKStub(cls, newName)

        val obj = gateway.instantiator.proxy(cls,
                objToCopy == null,
                moreInterfaces,
                stub)

        if (objToCopy != null) {
            copyFields(obj, objToCopy as Any)
        }

        gateway.stubs.put(obj, stub)

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
    }

}
