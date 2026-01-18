package io.mockk.impl.stub

import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.clearAllMocks
import io.mockk.clearAllStubsFromMemory
import io.mockk.every
import io.mockk.impl.instantiation.JvmObjectMockFactory
import io.mockk.mockk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockKStubMemoryTest {

    interface Service {
        fun compute(x: Int): Int
    }

    @AfterAll
    fun teardown() {
        clearAllStubsFromMemory()
    }

    @Test
    fun `stub repository does not grow after repeated clearAll`() {
        val serviceMocks = List(3) { mockk<Service>() }

        repeat(2) { round ->
            serviceMocks.forEach { svc ->
                every { svc.compute(round) } returns round
            }
        }

        clearAllStubsFromMemory()

        assertTrue(getStubList().isEmpty())
    }

    @Test
    fun `stub repository still has objects when clearing mocks`() {
        val serviceMocks = List(5) { mockk<Service>() }

        repeat(2) { round ->
            serviceMocks.forEach { svc ->
                every { svc.compute(round) } returns round
            }

            clearAllMocks()
        }

        serviceMocks.forEach { svc ->
            assertFailsWith<MockKException> { svc.compute(0) }
        }
        assertTrue(getStubList().isNotEmpty())
    }

    private fun getStubList(): List<Any> =
        (MockKGateway.implementation().objectMockFactory as JvmObjectMockFactory)
            .stubRepository.allStubs
}
