package io.mockk.impl

import kotlin.reflect.KClass

interface AnyValueGenerator {
    fun anyValue(cls: KClass<*>, orInstantiateVia: () -> Any?): Any?
}
