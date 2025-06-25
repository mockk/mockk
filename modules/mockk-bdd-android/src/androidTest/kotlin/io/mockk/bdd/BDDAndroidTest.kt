package io.mockk.bdd

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Android instrumentation tests for BDD-style API.
 * Verifies that the BDD functions work correctly in Android environment.
 */
@RunWith(AndroidJUnit4::class)
class BDDAndroidTest {
    
    interface AndroidTestService {
        fun getValue(): String
        fun setValue(value: String)
        suspend fun getAsyncValue(): List<String>
        fun processData(data: Any): Boolean
    }
    
    private lateinit var service: AndroidTestService
    
    @Before
    fun setup() {
        service = mockk()
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
    }
    
    @Test
    fun givenShouldWorkInAndroidTests() {
        // Given
        given { service.getValue() } returns "Android BDD Style"
        
        // When
        val result = service.getValue()
        
        // Then
        assertEquals("Android BDD Style", result)
    }
    
    @Test
    fun coGivenShouldWorkInAndroidTests() = runBlocking {
        // Given
        coGiven { service.getAsyncValue() } returns listOf("Android", "BDD", "Async")
        
        // When
        val result = service.getAsyncValue()
        
        // Then
        assertEquals(listOf("Android", "BDD", "Async"), result)
    }
    
    @Test
    fun thenShouldWorkInAndroidTests() {
        // Given
        val captureSlot = slot<String>()
        given { service.setValue(capture(captureSlot)) } returns Unit
        
        // When
        service.setValue("Android test value")
        
        // Then
        then { service.setValue("Android test value") }
        assertTrue(captureSlot.isCaptured)
        assertEquals("Android test value", captureSlot.captured)
    }
    
    @Test
    fun coThenShouldWorkInAndroidTests() = runBlocking {
        // Given
        coGiven { service.getAsyncValue() } returns listOf("test")
        
        // When
        service.getAsyncValue()
        
        // Then
        coThen { service.getAsyncValue() }
    }
    
    @Test
    fun thenWithParametersShouldWorkInAndroidTests() {
        // Given
        given { service.getValue() } returns "test"
        
        // When
        repeat(3) { service.getValue() }
        
        // Then
        then(exactly = 3) { service.getValue() }
        then(atLeast = 2) { service.getValue() }
        then(atMost = 5) { service.getValue() }
    }
    
    @Test
    fun shouldWorkWithAndroidSpecificObjects() {
        // Given
        given { service.processData(any()) } returns true
        
        // When
        val result = service.processData("Android specific data")
        
        // Then
        then { service.processData(any()) }
        assertEquals(true, result)
    }
} 