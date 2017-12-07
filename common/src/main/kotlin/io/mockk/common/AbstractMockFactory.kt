package io.mockk.common

import io.mockk.InternalPlatform
import io.mockk.InternalPlatform.toStr
import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.impl.Instantiator
import io.mockk.impl.Logger
import io.mockk.impl.MockKStub
import io.mockk.impl.SpyKStub
import java.lang.reflect.Modifier
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

abstract class AbstractMockFactory(val stubRepository: StubRepository,
                                   val instantiator: Instantiator) : MockKGateway.MockFactory {

    protected abstract fun <T : Any> newProxy(cls: KClass<out T>,
                                              moreInterfaces: Array<out KClass<*>>,
                                              stub: Stub,
                                              useDefaultConstructor: Boolean = false,
                                              instantiate: Boolean = false): T

    override fun <T : Any> mockk(cls: KClass<T>, name: String?, moreInterfaces: Array<out KClass<*>>): T {
        val newName = name ?: "#${newId()}"
        log.debug { "Creating mockk for ${cls.toStr()} name=$newName, moreInterfaces=${Arrays.toString(moreInterfaces)}" }

        val stub = MockKStub(cls, newName)

        log.trace { "Building proxy for ${cls.toStr()} hashcode=${InternalPlatform.hkd(cls)}" }
        val proxy = newProxy(cls, moreInterfaces, stub)

        stub.hashCodeStr = InternalPlatform.hkd(proxy)

        stubRepository.add(proxy, stub)

        return cls.cast(proxy)
    }

    override fun <T : Any> spyk(cls: KClass<T>?, objToCopy: T?, name: String?, moreInterfaces: Array<out KClass<*>>): T {
        val newName = name ?: "#${newId()}"
        log.debug { "Creating spyk for ${cls.toStr()} name=$newName, moreInterfaces=${Arrays.toString(moreInterfaces)}" }

        val actualCls = when {
            objToCopy != null -> objToCopy::class
            cls != null -> cls
            else -> throw MockKException("Either cls or objToCopy should not be null")
        }

        val stub = SpyKStub(actualCls, newName)

        val useDefaultConstructor = objToCopy == null

        log.trace { "Building proxy for ${actualCls.toStr()} hashcode=${InternalPlatform.hkd(actualCls)}" }

        val proxy = newProxy(actualCls, moreInterfaces, stub, useDefaultConstructor)

        stub.hashCodeStr = InternalPlatform.hkd(proxy)

        if (objToCopy != null) {
            InternalPlatform.copyFields(proxy, objToCopy)
        }

        stubRepository.add(proxy, stub)

        return actualCls.cast(proxy)
    }



    override fun childMock(cls: KClass<*>): Any {
        val stub = MockKStub(cls, "temporary mock");

        log.trace { "Building proxy for ${cls.toStr()} hashcode=${InternalPlatform.hkd(cls)}" }

        val proxy = newProxy(cls, arrayOf(), stub, instantiate = true)

        stub.hashCodeStr = InternalPlatform.hkd(proxy)

        return proxy
    }

    companion object {
        val idCounter = InternalPlatform.counter()

        fun newId(): Long = idCounter()

        val log = Logger<AbstractMockFactory>()
    }
}
