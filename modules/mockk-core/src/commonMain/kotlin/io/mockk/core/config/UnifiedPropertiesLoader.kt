package io.mockk.core.config

import java.util.Properties

/**
 * Default implementation of [PropertiesLoader] that loads MockK configuration from a unified location.
 *
 * This loader attempts to load properties from [UNIFIED_PROPERTIES_FILE] at the classpath root first.
 * If not found, it falls back to the legacy [LEGACY_PROPERTIES_FILE] location for backward compatibility.
 */
class UnifiedPropertiesLoader : PropertiesLoader {
    override fun loadProperties(): Properties {
        val properties = Properties()

        // Try to load from the unified location first (classpath root)
        val unifiedStream = Thread.currentThread().contextClassLoader
            .getResourceAsStream(UNIFIED_PROPERTIES_FILE)

        if (unifiedStream != null) {
            unifiedStream.use { properties.load(it) }
        } else {
            // Fallback to the legacy location for backward compatibility
            val legacyStream = UnifiedPropertiesLoader::class.java
                .getResourceAsStream(LEGACY_PROPERTIES_FILE)
            legacyStream?.use { properties.load(it) }
        }

        return properties
    }

    companion object {
        /**
         * The unified properties file location at the classpath root.
         */
        const val UNIFIED_PROPERTIES_FILE = "mockk.properties"

        /**
         * The legacy properties file location (io/mockk/settings.properties).
         */
        const val LEGACY_PROPERTIES_FILE = "settings.properties"
    }
}
