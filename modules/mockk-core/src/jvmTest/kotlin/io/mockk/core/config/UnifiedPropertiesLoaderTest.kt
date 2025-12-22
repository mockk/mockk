package io.mockk.core.config

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import java.io.File

class UnifiedPropertiesLoaderTest {
    private val loader = UnifiedPropertiesLoader

    @Test
    fun `loadProperties returns empty properties when no file exists`() {
        val properties = loader.loadProperties()
        assertTrue(properties.isEmpty, "Properties should be empty when no file exists")
    }

    @Test
    fun `read default config`() {
        writeToClasspath(UnifiedPropertiesLoader.UNIFIED_PROPERTIES_FILE, "name=default")
        val config = loader.loadProperties()
        assertEquals(config["name"], "default")
        deleteFromClasspath(UnifiedPropertiesLoader.UNIFIED_PROPERTIES_FILE)
    }

    @Test
    fun `read legacy config`() {
        writeToClasspath(UnifiedPropertiesLoader.LEGACY_PROPERTIES_FILE, "name=legacy")
        val config = loader.loadProperties()
        assertEquals(config["name"], "legacy")
        deleteFromClasspath(UnifiedPropertiesLoader.LEGACY_PROPERTIES_FILE)
    }

    @Test
    fun `default config takes precedence over legacy`() {
        writeToClasspath(UnifiedPropertiesLoader.UNIFIED_PROPERTIES_FILE, "name=default")
        writeToClasspath(UnifiedPropertiesLoader.LEGACY_PROPERTIES_FILE, "name=legacy")
        val config = loader.loadProperties()
        assertEquals(config["name"], "default")
        deleteFromClasspath(UnifiedPropertiesLoader.UNIFIED_PROPERTIES_FILE)
        deleteFromClasspath(UnifiedPropertiesLoader.LEGACY_PROPERTIES_FILE)
    }

    private fun writeToClasspath(path: String, content: String) {
        val file = File(this::class.java.getResource("/")!!.toURI()).resolve(path.removePrefix("/"))
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    private fun deleteFromClasspath(path: String) {
        val file = File(this::class.java.getResource("/")!!.toURI()).resolve(path.removePrefix("/"))
        file.delete()
    }
}