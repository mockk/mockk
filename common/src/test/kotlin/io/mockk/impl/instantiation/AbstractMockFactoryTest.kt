package io.mockk.impl.instantiation

import io.mockk.*
import io.mockk.impl.stub.Stub
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.impl.stub.StubRepository
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AbstractMockFactoryTest {
    lateinit var mockFactory: Factory
    lateinit var stubRepo: StubRepository
    lateinit var instantiator: AbstractInstantiator
    lateinit var gatewayAccess: StubGatewayAccess
    lateinit var mock: Mock

    @BeforeTest
    fun setUp() {
        stubRepo = mockk(relaxed = true)
        instantiator = mockk(relaxed = true)
        gatewayAccess = mockk(relaxed = true)
        mockFactory = spyk(Factory())
        mock = mockk(relaxed = true)
    }

    @Test
    fun whenRequestingMockkThenMockkCreated() {
        every {
            mockFactory.publicNewProxy<Mock>(any(), any(), any(), any(), any())
        } returns mock

        val mockk = mockFactory.mockk(Mock::class, "name", false, arrayOf())

        assertSame(mock, mockk)
    }

    @Test
    fun givenNameWhenRequestingMockkThenMockkWithNameCreated() {
        every {
            mockFactory.publicNewProxy<Mock>(any(), any(), any(), any(), any())
        } returns mock

        mockFactory.mockk(Mock::class, "name", false, arrayOf())

        val stubSlot = CapturingSlot<Stub>()
        verify {
            mockFactory.publicNewProxy(any(), any(), capture(stubSlot), any(), any())
        }

        assertEquals("name", stubSlot.captured.name)
    }

    @Test
    fun whenRequestingSpykThenSpykCreated() {
        every {
            mockFactory.publicNewProxy<Mock>(any(), any(), any(), any(), any())
        } returns mock

        val spyk = mockFactory.spyk(Mock::class, null, "name", arrayOf(), false)

        assertSame(mock, spyk)
    }

    @Test
    fun givenNameWhenRequestingSpykThenSpykWithNameCreated() {
        every {
            mockFactory.publicNewProxy<Mock>(any(), any(), any(), any(), any())
        } returns mock

        mockFactory.spyk(Mock::class, null, "name", arrayOf(), false)

        val stubSlot = CapturingSlot<Stub>()
        verify {
            mockFactory.publicNewProxy(any(), any(), capture(stubSlot), any(), any())
        }

        assertEquals("name", stubSlot.captured.name)
    }

    @Test
    fun whenRequestingChildMockkThenMockCreated() {
        every {
            mockFactory.publicNewProxy<Mock>(any(), any(), any(), any(), any())
        } returns mock

        val childMock = mockFactory.temporaryMock(Mock::class)

        assertSame(mock, childMock)
    }

    class Mock

    inner class Factory : AbstractMockFactory(stubRepo, instantiator, gatewayAccess) {
        fun <T : Any> publicNewProxy(
            cls: KClass<out T>,
            moreInterfaces: Array<out KClass<*>>,
            stub: Stub,
            useDefaultConstructor: Boolean,
            instantiate: Boolean
        ): T {
            throw AssertionError("fail")
        }

        override fun <T : Any> newProxy(
            cls: KClass<out T>,
            moreInterfaces: Array<out KClass<*>>,
            stub: Stub,
            useDefaultConstructor: Boolean,
            instantiate: Boolean
        ): T = publicNewProxy(cls, moreInterfaces, stub, useDefaultConstructor, instantiate)
    }
}