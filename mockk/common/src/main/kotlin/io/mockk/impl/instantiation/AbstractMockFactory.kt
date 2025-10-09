package io.mockk.impl.instantiation

import io.mockk.InternalPlatformDsl
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.MockKSettings
import io.mockk.impl.InternalPlatform
import io.mockk.impl.log.Logger
import io.mockk.impl.stub.*
import kotlin.reflect.KClass

abstract class AbstractMockFactory(
    val stubRepository: StubRepository,
    val instantiator: AbstractInstantiator,
    gatewayAccessIn: StubGatewayAccess
) : MockKGateway.MockFactory {

    val safeToString = gatewayAccessIn.safeToString
    val log = safeToString(Logger<AbstractMockFactory>())

    val gatewayAccess = gatewayAccessIn.copy(mockFactory = this)

    abstract fun <T : Any> newProxy(
        cls: KClass<out T>,
        moreInterfaces: Array<out KClass<*>>,
        stub: Stub,
        useDefaultConstructor: Boolean = false,
        instantiate: Boolean = false
    ): T

    override fun <T : Any> mockk(
        mockType: KClass<T>,
        name: String?,
        relaxed: Boolean,
        moreInterfaces: Array<out KClass<*>>,
        relaxUnitFun: Boolean
    ): T {
        val id = newId()
        val newName = (name ?: "") + "#$id"

        val stub = MockKStub(
            mockType,
            newName,
            relaxed || MockKSettings.relaxed,
            relaxUnitFun || MockKSettings.relaxUnitFun,
            gatewayAccess,
            true,
            MockType.REGULAR
        )

        if (moreInterfaces.isEmpty()) {
            log.debug { "Creating mockk for ${mockType.toStr()} name=$newName" }
        } else {
            log.debug { "Creating mockk for ${mockType.toStr()} name=$newName, moreInterfaces=${moreInterfaces.contentToString()}" }
        }

        log.trace { "Building proxy for ${mockType.toStr()} hashcode=${InternalPlatform.hkd(mockType)}" }
        val proxy = newProxy(mockType, moreInterfaces, stub)

        stub.hashCodeStr = InternalPlatform.hkd(proxy)

        stubRepository.add(proxy, stub)

        return proxy
    }

    override fun <T : Any> spyk(
        mockType: KClass<T>?,
        objToCopy: T?,
        name: String?,
        moreInterfaces: Array<out KClass<*>>,
        recordPrivateCalls: Boolean
    ): T {
        val id = newId()
        val newName = (name ?: "") + "#$id"

        val actualCls = when {
            objToCopy != null -> objToCopy::class
            mockType != null -> mockType
            else -> throw MockKException("Either mockType or objToCopy should not be null")
        }

        if (moreInterfaces.isEmpty()) {
            log.debug { "Creating spyk for ${actualCls.toStr()} name=$newName" }
        } else {
            log.debug { "Creating spyk for ${actualCls.toStr()} name=$newName, moreInterfaces=${moreInterfaces.contentToString()}" }
        }

        val stub = SpyKStub(
            actualCls,
            newName,
            gatewayAccess,
            recordPrivateCalls || MockKSettings.recordPrivateCalls,
            MockType.SPY
        )

        val useDefaultConstructor = objToCopy == null

        log.trace { "Building proxy for ${actualCls.toStr()} hashcode=${InternalPlatform.hkd(actualCls)}" }

        val proxy = newProxy(actualCls, moreInterfaces, stub, useDefaultConstructor)

        stub.hashCodeStr = InternalPlatform.hkd(proxy)

        if (objToCopy != null) {
            InternalPlatform.copyFields(proxy, objToCopy)
        }

        stubRepository.add(proxy, stub)

        return proxy
    }


    override fun temporaryMock(mockType: KClass<*>): Any {
        val stub = MockKStub(
            mockType,
            "temporary mock",
            gatewayAccess = gatewayAccess,
            recordPrivateCalls = true,
            mockType = MockType.TEMPORARY
        )

        log.trace { "Building proxy for ${mockType.toStr()} hashcode=${InternalPlatform.hkd(mockType)}" }

        val proxy = newProxy(mockType, arrayOf(), stub, instantiate = true)

        stub.hashCodeStr = InternalPlatform.hkd(proxy)

        return proxy
    }

    override fun isMock(value: Any) = gatewayAccess.stubRepository[value] != null

    companion object {
        val idCounter = InternalPlatformDsl.counter()

        fun newId(): Long = idCounter.increment() + 1
    }
}
