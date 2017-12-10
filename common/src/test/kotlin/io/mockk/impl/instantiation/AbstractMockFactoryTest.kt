package io.mockk.impl.instantiation

import io.mockk.CapturingSlot
import io.mockk.impl.stub.Stub
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.testEvery
import io.mockk.impl.testMockk
import io.mockk.impl.testSpyk
import io.mockk.impl.testVerify
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AbstractMockFactoryTest {
    lateinit var mockFactory: Factory
    lateinit var stubRepo: StubRepository
    lateinit var instantiator: AbstractInstantiator
    lateinit var anyValueGenerator: AnyValueGenerator
    lateinit var mock: Mock

    @BeforeTest
    fun setUp() {
        stubRepo = testMockk()
        instantiator = testMockk()
        anyValueGenerator = testMockk()
        mockFactory = testSpyk(Factory())
        mock = testMockk()
    }

    @Test
    fun whenRequestingMockkThenMockkCreated() {
        testEvery {
            mockFactory.publicNewProxy<Mock>(any(), any(), any(), any(), any())
        } returns mock

        val mockk = mockFactory.mockk(Mock::class, "name", false, arrayOf())

        assertSame(mock, mockk)
    }

    @Test
    fun givenNameWhenRequestingMockkThenMockkWithNameCreated() {
        testEvery {
            mockFactory.publicNewProxy<Mock>(any(), any(), any(), any(), any())
        } returns mock

        mockFactory.mockk(Mock::class, "name", false, arrayOf())

        val stubSlot = CapturingSlot<Stub>()
        testVerify {
            mockFactory.publicNewProxy(any(), any(), capture(stubSlot), any(), any())
        }

        assertEquals("name", stubSlot.captured.name)
    }

    @Test
    fun whenRequestingSpykThenSpykCreated() {
        testEvery {
            mockFactory.publicNewProxy<Mock>(any(), any(), any(), any(), any())
        } returns mock

        val spyk = mockFactory.spyk(Mock::class, null, "name", arrayOf())

        assertSame(mock, spyk)
    }

    @Test
    fun givenNameWhenRequestingSpykThenSpykWithNameCreated() {
        testEvery {
            mockFactory.publicNewProxy<Mock>(any(), any(), any(), any(), any())
        } returns mock

        mockFactory.spyk(Mock::class, null, "name", arrayOf())

        val stubSlot = CapturingSlot<Stub>()
        testVerify {
            mockFactory.publicNewProxy(any(), any(), capture(stubSlot), any(), any())
        }

        assertEquals("name", stubSlot.captured.name)
    }

    @Test
    fun whenRequestingChildMockkThenMockCreated() {
        testEvery {
            mockFactory.publicNewProxy<Mock>(any(), any(), any(), any(), any())
        } returns mock

        val childMock = mockFactory.childMock(Mock::class)

        assertSame(mock, childMock)
    }

    class Mock

    inner class Factory : AbstractMockFactory(stubRepo, instantiator, anyValueGenerator) {
        fun <T : Any> publicNewProxy(cls: KClass<out T>,
                                     moreInterfaces: Array<out KClass<*>>,
                                     stub: Stub,
                                     useDefaultConstructor: Boolean,
                                     instantiate: Boolean): T {
            throw AssertionError("fail")
        }

        override fun <T : Any> newProxy(cls: KClass<out T>,
                                        moreInterfaces: Array<out KClass<*>>,
                                        stub: Stub,
                                        useDefaultConstructor: Boolean,
                                        instantiate: Boolean): T = publicNewProxy(cls, moreInterfaces, stub, useDefaultConstructor, instantiate)
    }
}