package io.mockk.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class UnifiedPropertiesLoaderTest {
    @Test
    fun `missing resources produce independent empty results`() =
        IsolatedPropertiesLoader().use { fixture ->
            val first = fixture.propertiesLoader.loadProperties()
            val second = fixture.propertiesLoader.loadProperties()
            assertTrue(first.isEmpty)
            assertTrue(second.isEmpty)
            assertNotSame(first, second)
        }

    @Test
    fun `a caller cannot change another callers settings or restrictions`() =
        IsolatedPropertiesLoader().use { fixture ->
            fixture.unifiedContents = "relaxed=false\nmockk.restrictedClasses=original.Type"
            val first = fixture.propertiesLoader.loadProperties()
            first.setProperty("relaxed", "true")
            first.setProperty("mockk.restrictedClasses", "modified.Type")
            val second = fixture.propertiesLoader.loadProperties()
            assertEquals("false", second.getProperty("relaxed"))
            assertEquals("original.Type", second.getProperty("mockk.restrictedClasses"))
        }

    @Test
    fun `separate defining loaders keep independent configuration`() {
        IsolatedPropertiesLoader().use { first ->
            IsolatedPropertiesLoader().use { second ->
                first.unifiedContents = "name=first"
                second.unifiedContents = "name=second"
                assertEquals("first", first.propertiesLoader.loadProperties().getProperty("name"))
                assertEquals("second", second.propertiesLoader.loadProperties().getProperty("name"))
            }
        }
    }

    @Test
    fun `failed initial parsing can be retried with valid contents`() =
        IsolatedPropertiesLoader().use { fixture ->
            fixture.unifiedContents = "name=\\uBAD"
            fixture.legacyContents = "name=legacy"
            assertFailsWith<IllegalArgumentException> { fixture.propertiesLoader.loadProperties() }
            assertEquals(1, fixture.resourceReads)
            fixture.unifiedContents = "name=valid"
            assertEquals("valid", fixture.propertiesLoader.loadProperties().getProperty("name"))
        }
}
