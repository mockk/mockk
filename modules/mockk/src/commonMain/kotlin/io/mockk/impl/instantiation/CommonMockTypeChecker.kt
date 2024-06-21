package io.mockk.impl.instantiation

import io.mockk.MockKGateway.MockTypeChecker
import io.mockk.impl.stub.MockType
import io.mockk.impl.stub.SpyKStub
import io.mockk.impl.stub.StubRepository
import kotlin.reflect.KClass

open class CommonMockTypeChecker(
    val stubRepository: StubRepository,
    val isConstructorMockFun: (cls: KClass<*>) -> Boolean
) : MockTypeChecker {

    override fun isRegularMock(mock: Any): Boolean {
        val stub = stubRepository[mock]
            ?: return false

        return stub !is SpyKStub<*>
    }

    override fun isSpy(mock: Any): Boolean  = isOfMockType(mock, MockType.SPY)
    override fun isObjectMock(mock: Any): Boolean = isOfMockType(mock, MockType.OBJECT)
    override fun isStaticMock(mock: Any): Boolean = isOfMockType(mock, MockType.STATIC)

    override fun isConstructorMock(mock: Any) =
        if (mock is KClass<*>) isConstructorMockFun(mock) else false

    private fun isOfMockType(mock: Any, mockType: MockType): Boolean {
        val stub = stubRepository[mock] as? SpyKStub<*>
            ?: return false

        return stub.mockType == mockType
    }
}
