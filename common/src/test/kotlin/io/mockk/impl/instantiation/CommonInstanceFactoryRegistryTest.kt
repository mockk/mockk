package io.mockk.impl.instantiation

import io.mockk.MockKGateway
import io.mockk.MockKGateway.InstanceFactory
import io.mockk.impl.testMockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonInstanceFactoryRegistryTest {
    lateinit var factoryRegistry: CommonInstanceFactoryRegistry
    lateinit var factory: InstanceFactory

    @BeforeTest
    fun setUp() {
        factoryRegistry = CommonInstanceFactoryRegistry()
        factory = testMockk()
    }

    @Test
    fun givenInstanceFactoryWhenAddedToFactoryRegistryThenListOfFactoriesContainsIt() {
        factoryRegistry.registerFactory(factory)

        assertEquals(mutableListOf(factory), factoryRegistry.instanceFactories)
    }

    @Test
    fun givenInstanceFactoryWhenAddedAndRemovedToFactoryRegistryThenListOfFactoriesIsEmpty() {
        factoryRegistry.registerFactory(factory)
        factoryRegistry.deregisterFactory(factory)

        assertTrue(factoryRegistry.instanceFactories.isEmpty())
    }
}