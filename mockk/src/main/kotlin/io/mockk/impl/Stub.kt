package io.mockk.impl

import io.mockk.*
import kotlin.reflect.KClass

interface Stub {
    val name: String

    val type: KClass<*>

    fun addAnswer(matcher: InvocationMatcher, answer: Answer<*>)

    fun answer(invocation: Invocation): Any?

    fun childMockK(call: MatchedCall): Any?

    fun recordCall(invocation: Invocation)

    fun allRecordedCalls(): List<Invocation>

    fun clear(answers: Boolean, calls: Boolean, childMocks: Boolean)

    fun handleInvocation(self: Any,
                         method: MethodDescription,
                         originalCall: () -> Any?,
                         args: Array<out Any?>): Any?

    fun toStr(): String
}
