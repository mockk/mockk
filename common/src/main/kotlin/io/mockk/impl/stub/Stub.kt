package io.mockk.impl.stub

import io.mockk.Answer
import io.mockk.Invocation
import io.mockk.InvocationMatcher
import io.mockk.MethodDescription
import kotlin.reflect.KClass

interface Stub {
    val name: String

    val type: KClass<*>

    fun addAnswer(matcher: InvocationMatcher, answer: Answer<*>): AdditionalAnswerOpportunity

    fun answer(invocation: Invocation): Any?

    fun childMockK(matcher: InvocationMatcher, childType: KClass<*>): Any

    fun recordCall(invocation: Invocation)

    fun allRecordedCalls(): List<Invocation>

    fun clear(answers: Boolean, calls: Boolean, childMocks: Boolean)

    fun handleInvocation(
        self: Any,
        method: MethodDescription,
        originalCall: () -> Any?,
        args: Array<out Any?>
    ): Any?

    fun toStr(): String

    fun stdObjectAnswer(invocation: Invocation): Any?
}

