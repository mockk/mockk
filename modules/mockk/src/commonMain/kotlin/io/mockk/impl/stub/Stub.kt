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

    /**
     * Answers [invocation] the same way [answer] does, but with the call recording interposed
     * via [recordCallStep]: the step runs after the matched answer's capture slots are filled
     * and before the answer itself executes.
     *
     * This ordering makes the call visible to verifiers (the recorded-calls lists are what a
     * `verify(timeout = ...)` polls and what wakes its await session) only once its capture
     * slots are filled, so a timeout verification can no longer succeed while the answering
     * thread has not yet captured the arguments (issue #1169).
     *
     * The step must be invoked on every path: when the answer matched and when it did not
     * (default answer / no answer), mirroring the unconditional recording [answer] callers do.
     */
    fun answerWithRecordStep(
        invocation: Invocation,
        recordCallStep: () -> Unit,
    ): Any?

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

    /**
     * Marks the answer registered for [matcher] as lenient, meaning that
     * `checkUnnecessaryStub` will not report it even if it is never matched to an
     * invocation (issue #1272).
     */
    fun markAnswerLenient(matcher: InvocationMatcher)

    fun verifiedCalls(): List<Invocation>

    fun matcherUsages(): Map<InvocationMatcher, Int>

    /**
     * Matchers of the answers explicitly marked as lenient, which `checkUnnecessaryStub`
     * should skip when looking for stubs with zero usages (issue #1272).
     */
    fun lenientMatchers(): Set<InvocationMatcher>

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
