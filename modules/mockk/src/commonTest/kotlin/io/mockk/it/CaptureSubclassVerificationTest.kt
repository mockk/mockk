package io.mockk.it

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyCount
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * See issue #774.
 */
class CaptureSubclassVerificationTest {

    interface Interface

    class Subclass1 : Interface

    class Subclass2 : Interface

    interface Service {
        fun method(obj: Interface)
    }

    @Test
    fun `test unordered`() {
        val service = mockk<Service> {
            every { method(any()) } just Runs
        }

        service.method(Subclass1())
        service.method(Subclass2())

        val slot = slot<Subclass2>()
        verify(exactly = 1) { service.method(capture(slot)) }
        assertTrue(slot.isCaptured)
        assertIs<Subclass2>(slot.captured)
    }

    @Test
    fun `test ordered`() {
        val service = mockk<Service> {
            every { method(any()) } just Runs
        }

        service.method(Subclass1())
        service.method(Subclass2())

        val slot = slot<Subclass2>()
        verifyOrder {
            service.method(any())
            service.method(capture(slot))
        }
        assertTrue(slot.isCaptured)
        assertIs<Subclass2>(slot.captured)
    }

    @Test
    fun `test sequence`() {
        val service = mockk<Service> {
            every { method(any()) } just Runs
        }

        service.method(Subclass1())
        service.method(Subclass2())

        val slot = slot<Subclass2>()
        verifySequence {
            service.method(any())
            service.method(capture(slot))
        }
        assertTrue(slot.isCaptured)
        assertIs<Subclass2>(slot.captured)
    }

    @Test
    fun `test count`() {
        val service = mockk<Service> {
            every { method(any()) } just Runs
        }

        service.method(Subclass1())
        service.method(Subclass2())

        val slot = slot<Subclass2>()
        verifyCount {
            2 * { service.method(any()) }
            1 * { service.method(capture(slot)) }
        }
        assertTrue(slot.isCaptured)
        assertIs<Subclass2>(slot.captured)
    }
}
