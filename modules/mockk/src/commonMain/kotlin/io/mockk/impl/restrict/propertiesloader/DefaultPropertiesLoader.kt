package io.mockk.impl.restrict.propertiesloader

import io.mockk.core.config.UnifiedPropertiesLoader
import java.util.Properties

/**
 * Default implementation of [PropertiesLoader] that delegates to [UnifiedPropertiesLoader].
 *
 * @deprecated Use [UnifiedPropertiesLoader] directly from mockk-core.
 */
@Deprecated(
    message = "Use UnifiedPropertiesLoader from mockk-core instead",
    replaceWith = ReplaceWith("UnifiedPropertiesLoader()", "io.mockk.core.config.UnifiedPropertiesLoader")
)
class DefaultPropertiesLoader : PropertiesLoader {
    private val unifiedLoader = UnifiedPropertiesLoader()

    override fun loadProperties(): Properties {
        return unifiedLoader.loadProperties()
    }
}
