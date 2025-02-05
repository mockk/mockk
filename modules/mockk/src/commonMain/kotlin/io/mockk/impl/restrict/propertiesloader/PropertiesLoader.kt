package io.mockk.impl.restrict.propertiesloader

import java.util.Properties

interface PropertiesLoader {
    fun loadProperties(): Properties
}