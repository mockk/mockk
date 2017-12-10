package io.mockk.impl.instantiation

import io.mockk.MockKGateway.InstanceFactory
import io.mockk.impl.testEvery
import io.mockk.impl.testMockk
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AbstractInstantiatorTest {
    lateinit var instantiator: AbstractInstantiator
    lateinit var registry: CommonInstanceFactoryRegistry
    lateinit var factory: InstanceFactory


    @BeforeTest
    fun setUp() {
        registry = testMockk()
        factory = testMockk()
        instantiator = object : AbstractInstantiator(registry) {
            override fun <T : Any> instantiate(cls: KClass<T>): T = throw AssertionError("instantiate called")
        }
    }

    @Test
    fun givenSuitableInstantiationFactoryForIntWhenInstantanceRequestedThenInstanceIsCreated() {
        testEvery { registry.instanceFactories } returns listOf(factory)

        testEvery { factory.instantiate(Int::class) } returns 5

        val result = instantiator.instantiateViaInstanceFactoryRegistry(Int::class, { 6 })

        assertEquals(5, result)
    }

    @Test
    fun givenNoSuitableInstantiationFactoryWhenInstanceRequestedThenInstanceIsReturnedViaFallback() {
        testEvery { registry.instanceFactories } returns listOf(factory)

        testEvery { factory.instantiate(String::class) } returns null

        val result = instantiator.instantiateViaInstanceFactoryRegistry(String::class, { "abc" })

        assertEquals("abc", result)
    }
}