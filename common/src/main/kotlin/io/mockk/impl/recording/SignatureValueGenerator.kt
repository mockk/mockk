package io.mockk.impl.recording

import kotlin.reflect.KClass

interface SignatureValueGenerator {
    fun <T : Any> signatureValue(cls: KClass<T>, orInstantiateVia: () -> T): T
}