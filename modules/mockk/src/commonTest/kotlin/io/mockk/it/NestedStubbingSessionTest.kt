package io.mockk.it

import io.mockk.MockKException
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Regression tests for nested stubbing sessions, see
 * https://github.com/mockk/mockk/issues/1377
 *
 * A recorded block that itself initializes a mock (for example through a lazy
 * delegate) starts a nested stubbing session on the shared recorder. The nested
 * session must not break the outer one.
 */
class NestedStubbingSessionTest {
    interface Dependency {
        fun toTest(): Int
    }

    class Service {
        fun compute(value: Int): Int = value + 1
    }

    @Test
    fun `restubbing a lazily initialized mock does not fail`() {
        val mock by lazy {
            mockk<Dependency> {
                every { toTest() } returns 0
            }
        }

        every { mock.toTest() } returns 1

        assertEquals(1, mock.toTest())

        verify { mock.toTest() }
    }

    @Test
    fun `stubbing another mock inside an every block does not break the outer stubbing`() {
        val inner = mockk<Dependency>()
        val outer = mockk<Service>()

        every {
            // Nested session started by the inner stubbing while the outer block is recorded.
            every { inner.toTest() } returns 0
            outer.compute(1)
        } returns 42

        assertEquals(0, inner.toTest())
        assertEquals(42, outer.compute(1))
    }

    @Test
    fun `verification inside an every block does not break the outer stubbing`() {
        val mock = mockk<Dependency> {
            every { toTest() } returns 5
        }
        mock.toTest()

        val service = mockk<Service>()

        every {
            verify { mock.toTest() }
            service.compute(2)
        } returns 7

        assertEquals(7, service.compute(2))
    }

    @Test
    fun `stubbing inside a verification block does not break the verification`() {
        val verified = mockk<Dependency> {
            every { toTest() } returns 5
        }
        verified.toTest()

        val inner = mockk<Dependency>()

        verify {
            every { inner.toTest() } returns 3
            verified.toTest()
        }
    }

    @Test
    fun `failed nested stubbing does not leave the recorder broken`() {
        val outer = mockk<Service>()
        val inner = mockk<Dependency>()

        assertFailsWith<MockKException> {
            every {
                // Nested every without any mocked calls.
                every { inner }
                outer.compute(1)
            } returns 42
        }

        // The recorder must still work for subsequent sessions.
        every { outer.compute(3) } returns 30
        assertEquals(30, outer.compute(3))
        verify { outer.compute(3) }
    }

    @Test
    fun `coverification nested inside stubbing does not break the outer stubbing`() {
        val verified = mockk<Dependency> {
            every { toTest() } returns 5
        }
        verified.toTest()

        val service = mockk<Service>()

        every {
            coVerify { verified.toTest() }
            service.compute(4)
        } returns 14

        assertEquals(14, service.compute(4))
    }
}
