package io.mockk.junit5

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class MockKExtensionWithoutRequireParallelTestingTest {
    @MockK
    private lateinit var car: Car

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    companion object {
        @JvmStatic
        @AfterAll
        fun afterAll() {
            Dispatchers.shutdown()
        }
    }

    @Test
    fun `given car when test without require parallel testing execution returns successfully`() = runTest {
        // Given
        every { car.drive() } returns "driving"

        // When
        val result = car.drive()

        // Then
        verify { car.drive() }
        assert(result == "driving")
    }
}