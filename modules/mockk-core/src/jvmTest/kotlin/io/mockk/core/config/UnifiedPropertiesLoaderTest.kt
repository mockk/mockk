package io.mockk.core.config

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnifiedPropertiesLoaderTest {
    @Test
    fun `loadProperties returns empty properties when no file exists`() {
        val properties = UnifiedPropertiesLoader.loadProperties()
        assertTrue(properties.isEmpty, "Properties should be empty when no file exists")
    }

    @Test
    fun `read default config`() {
        writeToClasspath(UnifiedPropertiesLoader.UNIFIED_PROPERTIES_FILE, "name=default")
        val config = UnifiedPropertiesLoader.loadProperties()
        assertEquals(config["name"], "default")
        deleteFromClasspath(UnifiedPropertiesLoader.UNIFIED_PROPERTIES_FILE)
    }

    @Test
    fun `read legacy config`() {
        writeToClasspath(UnifiedPropertiesLoader.LEGACY_PROPERTIES_FILE, "name=legacy")
        val config = UnifiedPropertiesLoader.loadProperties()
        assertEquals(config["name"], "legacy")
        deleteFromClasspath(UnifiedPropertiesLoader.LEGACY_PROPERTIES_FILE)
    }

    @Test
    fun `default config takes precedence over legacy`() {
        writeToClasspath(UnifiedPropertiesLoader.UNIFIED_PROPERTIES_FILE, "name=default")
        writeToClasspath(UnifiedPropertiesLoader.LEGACY_PROPERTIES_FILE, "name=legacy")
        val config = UnifiedPropertiesLoader.loadProperties()
        assertEquals(config["name"], "default")
        deleteFromClasspath(UnifiedPropertiesLoader.UNIFIED_PROPERTIES_FILE)
        deleteFromClasspath(UnifiedPropertiesLoader.LEGACY_PROPERTIES_FILE)
    }

    private fun writeToClasspath(
        path: String,
        content: String,
    ) {
        resolveFromClasspath(path).run {
            parent?.let { Files.createDirectories(it) }
            Files.writeString(this, content)
        }
    }

    private fun deleteFromClasspath(path: String) {
        Files.delete(resolveFromClasspath(path))
    }

    private fun resolveFromClasspath(path: String): Path = getRoot().resolve(path.removePrefix("/"))

    private fun getRoot(): Path = Path.of(javaClass.getResource("/")!!.toURI())
}
