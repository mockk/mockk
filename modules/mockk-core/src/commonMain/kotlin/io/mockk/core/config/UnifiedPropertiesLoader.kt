package io.mockk.core.config

import java.util.Properties

/**
 * Default implementation of [PropertiesLoader] that loads MockK configuration from a unified location.
 *
 * This loader attempts to load properties from [UNIFIED_PROPERTIES_FILE] at the classpath root first.
 * If not found, it falls back to the legacy [LEGACY_PROPERTIES_FILE] location for backward compatibility.
 */
object UnifiedPropertiesLoader : PropertiesLoader {
    override fun loadProperties(): Properties =
        Properties().apply {
            UnifiedPropertiesLoader::class.java
                .run {
                    getResourceAsStream(UNIFIED_PROPERTIES_FILE)
                        // Fallback to the legacy file for backward compatibility
                        ?: getResourceAsStream(LEGACY_PROPERTIES_FILE)
                }?.use(::load)
        }

    /**
     * The unified properties file location.
     */
    internal const val UNIFIED_PROPERTIES_FILE = "/mockk.properties"

    /**
     * The legacy properties file location.
     */
    internal const val LEGACY_PROPERTIES_FILE = "/io/mockk/settings.properties"
}
