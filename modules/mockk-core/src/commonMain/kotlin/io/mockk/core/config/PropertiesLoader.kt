package io.mockk.core.config

import java.util.Properties

/**
 * Interface for loading MockK configuration properties.
 */
interface PropertiesLoader {
    /**
     * Loads and returns the MockK configuration properties.
     */
    fun loadProperties(): Properties
}
