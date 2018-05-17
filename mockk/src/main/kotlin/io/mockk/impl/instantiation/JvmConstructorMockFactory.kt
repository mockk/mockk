package io.mockk.impl.instantiation

import io.mockk.MockKGateway.ConstructorMockFactory
import io.mockk.impl.stub.StubRepository
import kotlin.reflect.KClass

class JvmConstructorMockFactory(stubRepo: StubRepository) : ConstructorMockFactory {
    override fun constructorMockk(cls: KClass<*>, recordPrivateCalls: Boolean) {
        TODO("not implemented")
    }

    override fun constructorUnMockk(cls: KClass<*>) {
        TODO("not implemented")
    }

    override fun <T : Any> mockPlaceholder(cls: KClass<T>): T {
        TODO("not implemented")
    }

    override fun clear(type: KClass<*>, answers: Boolean, recordedCalls: Boolean, childMocks: Boolean) {
        TODO("not implemented")
    }

}
