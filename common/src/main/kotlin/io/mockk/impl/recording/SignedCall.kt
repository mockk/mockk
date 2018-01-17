package io.mockk.impl.recording

import io.mockk.MethodDescription
import kotlin.reflect.KClass

data class SignedCall(
    val retValue: Any?,
    val isRetValueMock: Boolean,
    val retType: KClass<*>,
    val self: Any,
    val method: MethodDescription,
    val args: List<Any?>,
    val invocationStr: String,
    val matchers: List<SignedMatcher>
)
