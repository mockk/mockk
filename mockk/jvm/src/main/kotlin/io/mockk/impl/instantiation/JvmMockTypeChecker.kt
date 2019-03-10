package io.mockk.impl.instantiation

import io.mockk.impl.stub.StubRepository
import kotlin.reflect.KClass

class JvmMockTypeChecker(
    stubRepository: StubRepository,
    isConstructorMockFun: (cls: KClass<*>) -> Boolean
) : CommonMockTypeChecker(stubRepository, isConstructorMockFun) {

    override fun isStaticMock(mock: Any): Boolean {
        val mockCls = if (mock is KClass<*>) mock.java else mock
        return super.isStaticMock(mockCls)
    }

    override fun isConstructorMock(mock: Any) = when (mock) {
        is Class<*> -> super.isConstructorMock(mock.kotlin)
        else -> super.isConstructorMock(mock)
    }
}
