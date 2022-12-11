package io.mockk.impl.instantiation

import io.mockk.MockKGateway.InstanceFactory
import io.mockk.every
import io.mockk.mockk
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
        registry = mockk(relaxed = true)
        factory = mockk(relaxed = true)
        instantiator = object : AbstractInstantiator(registry) {
            override fun <T : Any> instantiate(cls: KClass<T>): T = throw AssertionError("instantiate called")
        }
    }

    @Test
    fun givenSuitableInstantiationFactoryForIntWhenInstantanceRequestedThenInstanceIsCreated() {
        every { registry.instanceFactories } returns listOf(factory)

        every { factory.instantiate(Int::class) } returns 5

        val result = instantiator.instantiateViaInstanceFactoryRegistry(Int::class) { 6 }

        assertEquals(5, result)
    }

    @Test
    fun givenNoSuitableInstantiationFactoryWhenInstanceRequestedThenInstanceIsReturnedViaFallback() {
        every { registry.instanceFactories } returns listOf(factory)

        every { factory.instantiate(String::class) } returns null

        val result = instantiator.instantiateViaInstanceFactoryRegistry(String::class) { "abc" }

        assertEquals("abc", result)
    }
}
