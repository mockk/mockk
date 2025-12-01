package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Test for GitHub issue #1295:
 * ClassNotFoundException when mocking companion object with @JvmStatic external method returning Array<String>
 *
 * The issue was that Kotlin reflection throws ClassNotFoundException (or NullPointerException in some versions)
 * when trying to access kotlinFunction for methods returning Array<T>. The fix catches Throwable in isKotlinInline()
 * to gracefully handle all reflection failures.
 */
class JvmStaticExternalMethodTest {

    /**
     * A class that simulates the problematic pattern from the issue:
     * - Private constructor
     * - Companion object with instance property
     * - @JvmStatic external method returning Array<String>
     *
     * Note: We can't use actual 'external' modifier without native library,
     * but the issue is specifically with reflection on Array<String> return type methods.
     */
    class Settings private constructor() {
        companion object {
            val instance: Settings = Settings()
            
            // This method has Array<String> return type which causes reflection issues
            @JvmStatic
            fun methodReturningStringArray(): Array<String> = arrayOf("test")
        }
    }

    private val settings: Settings = mockk()

    @AfterEach
    fun cleanup() {
        unmockkObject(Settings.Companion)
    }

    /**
     * This test reproduces issue #1295.
     * Before the fix, this test would fail with:
     * java.lang.ClassNotFoundException: kotlin.Array
     * or
     * java.lang.NullPointerException: loadClass(...) must not be null
     */
    @Test
    fun `should mock companion object with JvmStatic method returning Array String`() {
        mockkObject(Settings.Companion)
        
        // This line triggered the exception before the fix
        every { Settings.instance } returns settings
        
        assertEquals(settings, Settings.instance)
    }

    /**
     * Additional test to verify mocking of the actual method with Array return type works
     */
    @Test
    fun `should mock method returning Array String in companion object`() {
        mockkObject(Settings.Companion)
        
        val expected = arrayOf("mocked", "values")
        every { Settings.methodReturningStringArray() } returns expected
        
        val result = Settings.methodReturningStringArray()
        assertEquals(expected.toList(), result.toList())
    }
}
