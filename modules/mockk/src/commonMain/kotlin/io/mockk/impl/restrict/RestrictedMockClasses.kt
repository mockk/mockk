package io.mockk.impl.restrict

import io.mockk.MockKSettings
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger

object RestrictedMockClasses {
    private val logger = Logger.getLogger(RestrictedMockClasses::class.java.name)

    private val defaultRestrictedTypes: Set<Class<*>> = setOf(
        System::class.java,

        Collection::class.java,
        Map::class.java,

        File::class.java,
        Path::class.java
    )

    private val userDefinedRestrictedTypes = mutableSetOf<Class<*>>()

    fun handleRestrictedMocking(clazz: Class<*>) {
        if (isRestricted(clazz)) {
            if (MockKSettings.disallowMockingRestrictedClasses) {
                throw IllegalArgumentException("Cannot mock restricted class: ${clazz.name}")
            } else {
                logger.warning("Warning: Attempting to mock a restricted class (${clazz.name}). This is usually a bad practice.")
            }
        }
    }

    fun isRestricted(clazz: Class<*>): Boolean {
        return defaultRestrictedTypes.any { it.isAssignableFrom(clazz) } ||
                userDefinedRestrictedTypes.any { it.isAssignableFrom(clazz) }
    }

    fun addRestrictedType(clazz: Class<*>) {
        userDefinedRestrictedTypes.add(clazz)
    }

    fun removeRestrictedType(clazz: Class<*>) {
        userDefinedRestrictedTypes.remove(clazz)
    }

    fun clearUserDefinedRestrictions() {
        userDefinedRestrictedTypes.clear()
    }
}