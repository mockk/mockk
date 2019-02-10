package io.mockk.impl.instantiation

import io.mockk.MockKGateway.MockTypeChecker
import io.mockk.impl.stub.MockType
import io.mockk.impl.stub.SpyKStub
import io.mockk.impl.stub.StubRepository
import kotlin.reflect.KClass

class JvmMockTypeChecker(
    val stubRepository: StubRepository,
    val isConstructorMockFun: (cls: KClass<*>) -> Boolean
) : MockTypeChecker {

    override fun isRegularMock(mock: Any): Boolean {
        val stub = stubRepository[mock]
            ?: return false

        return stub !is SpyKStub<*>
    }

    override fun isSpy(mock: Any): Boolean {

        val stub = stubRepository[mock] as? SpyKStub<*>
            ?: return false

        return stub.mockType == MockType.SPY
    }

    override fun isObjectMock(mock: Any): Boolean {
        val stub = stubRepository[mock] as? SpyKStub<*>
            ?: return false

        return stub.mockType == MockType.OBJECT
    }

    override fun isStaticMock(mock: Any): Boolean {
        val mockCls = if (mock is KClass<*>) mock.java else mock

        val stub = stubRepository[mockCls] as? SpyKStub<*>
            ?: return false

        return stub.mockType == MockType.STATIC
    }

    override fun isConstructorMock(mock: Any) = when (mock) {
        is KClass<*> -> isConstructorMockFun(mock)
        is Class<*> -> isConstructorMockFun(mock.kotlin)
        else -> false
    }
}
