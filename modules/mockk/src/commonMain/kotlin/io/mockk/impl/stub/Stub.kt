package io.mockk.impl.stub

import io.mockk.Answer
import io.mockk.BackingFieldValueProvider
import io.mockk.Invocation
import io.mockk.InvocationMatcher
import io.mockk.MethodDescription
import io.mockk.MockKGateway
import io.mockk.MockKGateway.ExclusionParameters
import io.mockk.impl.platform.Disposable
import kotlin.reflect.KClass

interface Stub : Disposable {
    val name: String

    val type: KClass<*>

    val threadId: Long

    fun addAnswer(
        matcher: InvocationMatcher,
        answer: Answer<*>,
    )

    fun answer(invocation: Invocation): Any?

    fun childMockK(
        matcher: InvocationMatcher,
        childType: KClass<*>,
    ): Any

    fun recordCall(invocation: Invocation)

    fun allRecordedCalls(): List<Invocation>

    fun allRecordedCalls(method: MethodDescription): List<Invocation>

    fun excludeRecordedCalls(
        params: ExclusionParameters,
        matcher: InvocationMatcher,
    )

    fun markCallVerified(invocation: Invocation)

    /**
     * Marks the answer registered for [matcher] as used, without it being matched to an
     * invocation. Needed for getter stubs whose constant answer is written into the
     * mock's backing field: such a stub changes the mock's behavior without the getter
     * ever being intercepted, so its usage count would stay at zero and
     * `checkUnnecessaryStub` would wrongly report it (issue #1345).
     */
    fun markAnswerUsed(matcher: InvocationMatcher)

    fun verifiedCalls(): List<Invocation>

    fun matcherUsages(): Map<InvocationMatcher, Int>

    fun clear(options: MockKGateway.ClearOptions)

    fun handleInvocation(
        self: Any,
        method: MethodDescription,
        originalCall: () -> Any?,
        args: Array<out Any?>,
        fieldValueProvider: BackingFieldValueProvider,
    ): Any?

    fun toStr(): String

    fun stdObjectAnswer(invocation: Invocation): Any?
}
