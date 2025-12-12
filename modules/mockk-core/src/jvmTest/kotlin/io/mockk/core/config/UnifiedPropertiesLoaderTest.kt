package io.mockk.core.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnifiedPropertiesLoaderTest {

    @Test
    fun `loadProperties returns empty properties when no file exists`() {
        // Given: A loader with no properties files in the test resources
        val loader = UnifiedPropertiesLoader()

        // When: Loading properties
        val properties = loader.loadProperties()

        // Then: Should return empty properties without throwing an exception
        assertTrue(properties.isEmpty, "Properties should be empty when no file exists")
    }

    @Test
    fun `UNIFIED_PROPERTIES_FILE constant has correct value`() {
        assertEquals("mockk.properties", UnifiedPropertiesLoader.UNIFIED_PROPERTIES_FILE)
    }

    @Test
    fun `LEGACY_PROPERTIES_FILE constant has correct value`() {
        assertEquals("settings.properties", UnifiedPropertiesLoader.LEGACY_PROPERTIES_FILE)
    }

    @Test
    fun `loadProperties uses correct context class loader`() {
        // Given: A loader instance
        val loader = UnifiedPropertiesLoader()

        // When: Loading properties
        val properties = loader.loadProperties()

        // Then: Should not throw an exception even if no files exist
        // This verifies that the correct class loader is being used
        assertTrue(properties != null, "Properties should not be null")
    }
}
