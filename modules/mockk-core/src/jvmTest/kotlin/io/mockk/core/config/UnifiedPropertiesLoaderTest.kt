package io.mockk.core.config

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    private fun writeToClasspath(
        path: String,
        content: String,
    ) {
        getRoot().resolve(path.removePrefix("/")).run {
            parent?.let { Files.createDirectories(it) }
            writeText(content)
        }
    }

    private fun deleteFromClasspath(path: String) {
        getRoot().resolve(path.removePrefix("/")).deleteExisting()
    }

    private fun getRoot(): Path = Path.of(this::class.java.getResource("/")!!.toURI())
}
