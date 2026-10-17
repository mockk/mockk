package io.mockk.core.config

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnifiedPropertiesLoaderDirectoryTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `unified file wins over legacy and later edits or removal do not change the snapshot`() {
        write("mockk.properties", "name=unified")
        write("io/mockk/settings.properties", "name=legacy")
        IsolatedPropertiesLoader(directory).use { fixture ->
            assertEquals("unified", fixture.propertiesLoader.loadProperties().getProperty("name"))
            write("mockk.properties", "name=changed")
            assertEquals("unified", fixture.propertiesLoader.loadProperties().getProperty("name"))
            Files.delete(directory.resolve("mockk.properties"))
            assertEquals("unified", fixture.propertiesLoader.loadProperties().getProperty("name"))
            assertEquals(1, fixture.resourceReads)
        }
    }

    @Test
    fun `legacy file remains selected if a unified file is added after the first lookup`() {
        write("io/mockk/settings.properties", "name=legacy")
        IsolatedPropertiesLoader(directory).use { fixture ->
            assertEquals("legacy", fixture.propertiesLoader.loadProperties().getProperty("name"))
            write("mockk.properties", "name=added")
            assertEquals("legacy", fixture.propertiesLoader.loadProperties().getProperty("name"))
            assertEquals(2, fixture.resourceReads)
        }
    }

    @Test
    fun `an empty unified file does not fall back to legacy`() {
        write("mockk.properties", "")
        write("io/mockk/settings.properties", "name=legacy")
        IsolatedPropertiesLoader(directory).use { fixture ->
            assertTrue(fixture.propertiesLoader.loadProperties().isEmpty)
            assertEquals(1, fixture.resourceReads)
        }
    }

    @Test
    fun `files added after an empty first lookup are visible only to a fresh defining loader`() {
        IsolatedPropertiesLoader(directory).use { fixture ->
            assertTrue(fixture.propertiesLoader.loadProperties().isEmpty)
            write("mockk.properties", "name=added")
            assertTrue(fixture.propertiesLoader.loadProperties().isEmpty)
            assertEquals(2, fixture.resourceReads)
            IsolatedPropertiesLoader(directory).use { fresh ->
                assertEquals("added", fresh.propertiesLoader.loadProperties().getProperty("name"))
            }
        }
    }

    private fun write(
        name: String,
        contents: String,
    ) {
        val file = directory.resolve(name)
        Files.createDirectories(file.parent)
        Files.write(file, contents.toByteArray(Charsets.ISO_8859_1))
    }
}
