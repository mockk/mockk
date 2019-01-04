package io.mockk.impl.stub

import io.mockk.*
import io.mockk.MockKGateway.ExclusionParameters
import io.mockk.impl.platform.Disposable
import kotlin.reflect.KClass

interface Stub : Disposable {
    val name: String

    val type: KClass<*>

    fun addAnswer(matcher: InvocationMatcher, answer: Answer<*>): AdditionalAnswerOpportunity

    fun answer(invocation: Invocation): Any?

    fun childMockK(matcher: InvocationMatcher, childType: KClass<*>): Any

    fun recordCall(invocation: Invocation)

    fun allRecordedCalls(): List<Invocation>

    fun allRecordedCalls(method: MethodDescription): List<Invocation>

    fun excludeRecordedCalls(params: ExclusionParameters, matcher: InvocationMatcher)

    fun markCallVerified(invocation: Invocation)

    fun verifiedCalls(): List<Invocation>

    fun clear(options: MockKGateway.ClearOptions)

    fun handleInvocation(
        self: Any,
        method: MethodDescription,
        originalCall: () -> Any?,
        args: Array<out Any?>,
        fieldValueProvider: BackingFieldValueProvider
    ): Any?

    fun toStr(): String

    fun stdObjectAnswer(invocation: Invocation): Any?
}

