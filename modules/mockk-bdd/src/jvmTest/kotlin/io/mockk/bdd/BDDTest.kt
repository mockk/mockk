package io.mockk.bdd

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for BDD-style API.
 * Verifies that the BDD functions correctly delegate to the MockK core functions.
 */
class BDDTest {
    interface TestService {
        fun getValue(): String

        fun setValue(value: String)

        suspend fun getAsyncValue(): List<String>
    }

    private lateinit var service: TestService

    @BeforeEach
    fun setup() {
        service = mockk()
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given should work like every`() {
        // Stub using original MockK API
        every { service.getValue() } returns "MockK"

        // Reset and stub using BDD API
        clearAllMocks()
        given { service.getValue() } returns "BDD Style"

        // Verify both APIs work the same
        assertEquals("BDD Style", service.getValue())
    }

    @Test
    fun `coGiven should work like coEvery`() =
        runBlocking {
            // Stub using original MockK API
            coEvery { service.getAsyncValue() } returns listOf("MockK")

            // Reset and stub using BDD API
            clearAllMocks()
            coGiven { service.getAsyncValue() } returns listOf("BDD Style")

            // Verify both APIs work the same
            assertEquals(listOf("BDD Style"), service.getAsyncValue())
        }

    @Test
    fun `then should work like verify`() {
        // Setup
        val captureSlot = slot<String>()
        given { service.setValue(capture(captureSlot)) } returns Unit

        // Action
        service.setValue("test value")

        // Verify using original MockK API
        verify { service.setValue("test value") }

        // Reset verification
        captureSlot.clear()

        // Action again
        service.setValue("BDD value")

        // Verify using BDD API
        then { service.setValue("BDD value") }

        // Check captured value
        assertTrue(captureSlot.isCaptured)
        assertEquals("BDD value", captureSlot.captured)
    }

    @Test
    fun `coThen should work like coVerify`() =
        runBlocking {
            // Setup
            coGiven { service.getAsyncValue() } returns listOf("async value")

            // Action
            service.getAsyncValue()

            // Verify using original MockK API
            coVerify { service.getAsyncValue() }

            // Reset and setup again
            clearAllMocks()
            coGiven { service.getAsyncValue() } returns listOf("BDD async")

            // Action again
            service.getAsyncValue()

            // Verify using BDD API
            coThen { service.getAsyncValue() }
        }

    @Test
    fun `then should support all parameters like verify`() {
        // Setup
        given { service.getValue() } returns "value"

        // Action multiple times
        repeat(3) { service.getValue() }

        // Verify exact count using BDD API
        then(exactly = 3) { service.getValue() }

        // Verify with atLeast
        then(atLeast = 2) { service.getValue() }

        // Verify with atMost
        then(atMost = 5) { service.getValue() }
    }
}
