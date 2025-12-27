package io.mockk.impl.restrict

import io.mockk.core.config.PropertiesLoader
import io.mockk.core.config.UnifiedPropertiesLoader
import java.util.Properties

class RestrictMockkConfiguration(
    propertiesLoader: PropertiesLoader = UnifiedPropertiesLoader,
) {
    val userDefinedRestrictedTypes: Set<String>
    val restrictedTypes: Set<String>
    val throwExceptionOnBadMock: Boolean

    init {
        val properties = propertiesLoader.loadProperties()
        userDefinedRestrictedTypes = loadRestrictedTypesFromConfig(properties)

        restrictedTypes = DEFAULT_RESTRICTED_CLAZZ + userDefinedRestrictedTypes

        // System Property takes precedence.
        throwExceptionOnBadMock = loadThrowExceptionSystemProperty()
            ?: loadThrowExceptionSetting(properties)
    }

    companion object {
        private const val RESTRICTED_MOCK_PROP_KEY = "mockk.throwExceptionOnBadMock"
        private val DEFAULT_RESTRICTED_CLAZZ =
            setOf(
                "java.lang.System",
                "java.util.Collection",
                "java.util.HashMap",
                "java.io.File",
                "java.nio.file.Path",
            )

        private fun loadRestrictedTypesFromConfig(properties: Properties): Set<String> =
            properties
                .getProperty("mockk.restrictedClasses", "")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

        private fun loadThrowExceptionSystemProperty(): Boolean? {
            val throwExceptionSystemProperty = System.getProperty(RESTRICTED_MOCK_PROP_KEY)
            if (throwExceptionSystemProperty.isNullOrEmpty()) {
                return null
            }
            return throwExceptionSystemProperty.toBoolean()
        }

        private fun loadThrowExceptionSetting(properties: Properties): Boolean =
            properties.getProperty(RESTRICTED_MOCK_PROP_KEY, "false").toBoolean()
    }
}
