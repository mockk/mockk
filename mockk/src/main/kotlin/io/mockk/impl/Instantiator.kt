package io.mockk.impl

import kotlin.reflect.KClass

interface Instantiator {
    fun <T : Any> instantiate(cls: KClass<T>): T
}
