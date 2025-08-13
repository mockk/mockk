package io.mockk.impl.restrict.propertiesloader

import java.util.Properties

class DefaultPropertiesLoader : PropertiesLoader {
    override fun loadProperties(): Properties {
        val properties = Properties()
        val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream("mockk.properties")
        resourceStream?.use { properties.load(it) }
        return properties
    }
}
