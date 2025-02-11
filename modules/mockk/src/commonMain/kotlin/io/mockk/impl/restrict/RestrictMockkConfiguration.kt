package io.mockk.impl.restrict

import io.mockk.impl.restrict.propertiesloader.DefaultPropertiesLoader
import io.mockk.impl.restrict.propertiesloader.PropertiesLoader
import java.util.*

class RestrictMockkConfiguration(propertiesLoader: PropertiesLoader = DefaultPropertiesLoader()) {
    val userDefinedRestrictedTypes: Set<String>
    val restrictedTypes: Set<String>
    val throwExceptionOnBadMock: Boolean

    init {
        val properties = propertiesLoader.loadProperties()
        userDefinedRestrictedTypes = loadRestrictedTypesFromConfig(properties)

        restrictedTypes = DEFAULT_RESTRICTED_CLAZZ + userDefinedRestrictedTypes
        throwExceptionOnBadMock = loadThrowExceptionSetting(properties)
    }

    companion object {
        private val DEFAULT_RESTRICTED_CLAZZ = setOf(
            "java.lang.System",
            "java.util.Collection",
            "java.util.HashMap",
            "java.io.File",
            "java.nio.file.Path",
        )

        private fun loadRestrictedTypesFromConfig(properties: Properties): Set<String> {
            return properties.getProperty("mockk.restrictedClasses", "")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        }

        private fun loadThrowExceptionSetting(properties: Properties): Boolean {
            return properties.getProperty("mockk.throwExceptionOnBadMock", "false").toBoolean()
        }
    }
}
