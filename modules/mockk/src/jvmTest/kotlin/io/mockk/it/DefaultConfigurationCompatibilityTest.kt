package io.mockk.it

import io.mockk.MockKSettings
import io.mockk.core.config.PropertiesLoader
import io.mockk.core.config.UnifiedPropertiesLoader
import io.mockk.impl.restrict.RestrictMockkConfiguration
import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultConfigurationCompatibilityTest {
    @Test
    fun changingMockKSettingsDoesNotChangeTheLoadedResource() {
        val originalSetting = MockKSettings.relaxed
        val resourceSetting = UnifiedPropertiesLoader.loadProperties().getProperty("relaxed")
        try {
            MockKSettings.setRelaxed(!originalSetting)
            assertEquals(!originalSetting, MockKSettings.relaxed)
            assertEquals(resourceSetting, UnifiedPropertiesLoader.loadProperties().getProperty("relaxed"))
        } finally {
            MockKSettings.setRelaxed(originalSetting)
        }
    }

    @Test
    fun changingSystemPropertyAffectsTheNextDefaultConfiguration() {
        val key = "mockk.throwExceptionOnBadMock"
        val previous = System.getProperty(key)
        try {
            System.setProperty(key, "true")
            assertTrue(RestrictMockkConfiguration().throwExceptionOnBadMock)
            System.setProperty(key, "false")
            assertFalse(RestrictMockkConfiguration().throwExceptionOnBadMock)
        } finally {
            if (previous == null) System.clearProperty(key) else System.setProperty(key, previous)
        }
    }

    @Test
    fun explicitCustomLoaderIsReadForEveryConfiguration() {
        var reads = 0
        val loader =
            object : PropertiesLoader {
                override fun loadProperties(): Properties =
                    Properties().apply {
                        reads++
                        setProperty("mockk.restrictedClasses", "example.Type$reads")
                    }
            }
        assertEquals(setOf("example.Type1"), RestrictMockkConfiguration(loader).userDefinedRestrictedTypes)
        assertEquals(setOf("example.Type2"), RestrictMockkConfiguration(loader).userDefinedRestrictedTypes)
        assertEquals(2, reads)
    }
}
