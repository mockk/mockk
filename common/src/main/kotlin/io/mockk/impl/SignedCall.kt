package io.mockk.impl

import io.mockk.Invocation
import io.mockk.Matcher
import kotlin.reflect.KClass

data class SignedCall(val retType: KClass<*>,
                      val invocation: Invocation,
                      val matchers: List<Matcher<*>>,
                      val signaturePart: List<Any>)