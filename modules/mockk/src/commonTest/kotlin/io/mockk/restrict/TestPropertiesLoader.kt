package io.mockk.restrict

import io.mockk.impl.restrict.propertiesloader.PropertiesLoader
import java.util.*

class TestPropertiesLoader(private val mockProperties: Map<String, String> = emptyMap()) : PropertiesLoader {
    override fun loadProperties(): Properties {
        val properties = Properties()
        mockProperties.forEach { (key, value) ->
            properties[key] = value
        }
        return properties
    }
}
