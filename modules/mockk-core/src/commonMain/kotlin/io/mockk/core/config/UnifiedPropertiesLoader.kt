package io.mockk.core.config

import java.util.Properties

/**
 * Loads unified configuration first, falling back to the legacy classpath location.
 * The first successful read is retained for this object's defining classloader.
 */
object UnifiedPropertiesLoader : PropertiesLoader {
    // Settings callers mutate Properties. Keep the parsed snapshot private so a caller's
    // changes cannot affect another caller or the cached classpath configuration.
    private val snapshot: Properties by lazy { readFromClasspath() }

    override fun loadProperties(): Properties = snapshot.clone() as Properties

    private fun readFromClasspath(): Properties =
        Properties().apply {
            UnifiedPropertiesLoader::class.java
                .run {
                    getResourceAsStream(UNIFIED_PROPERTIES_FILE)
                        ?: getResourceAsStream(LEGACY_PROPERTIES_FILE)
                }?.use(::load)
        }

    internal const val UNIFIED_PROPERTIES_FILE = "/mockk.properties"
    internal const val LEGACY_PROPERTIES_FILE = "/io/mockk/settings.properties"
}
