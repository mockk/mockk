package io.mockk.impl.stub

import io.mockk.*
import io.mockk.MockKGateway.ExclusionParameters
import io.mockk.impl.InternalPlatform
import kotlin.reflect.KClass

class ConstructorStub(
    val mock: Any,
    val representativeMock: Any,
    val stub: Stub,
    val recordPrivateCalls: Boolean
) : Stub {
    private val represent = identityMapOf(mock to representativeMock)
    private val revertRepresentation = identityMapOf(representativeMock to mock)

    private fun <K, V> identityMapOf(vararg pairs: Pair<K, V>): Map<K, V> =
        InternalPlatform.identityMap<K, V>()
            .also { map -> map.putAll(pairs) }

    override val name: String
        get() = stub.name

    override val type: KClass<*>
        get() = stub.type

    override val threadId: Long
        get() = stub.threadId

    override fun addAnswer(matcher: InvocationMatcher, answer: Answer<*>) =
        stub.addAnswer(matcher.substitute(represent), answer)

    override fun answer(invocation: Invocation) = stub.answer(
        invocation.substitute(represent)
    ).internalSubstitute(revertRepresentation)

    override fun childMockK(matcher: InvocationMatcher, childType: KClass<*>) =
        stub.childMockK(
            matcher.substitute(represent),
            childType
        )

    override fun recordCall(invocation: Invocation) {
        val record = if (recordPrivateCalls)
            true
        else
            !invocation.method.privateCall

        if (record) {
            stub.recordCall(invocation.substitute(represent))
        }
    }

    override fun excludeRecordedCalls(params: ExclusionParameters, matcher: InvocationMatcher) {
        stub.excludeRecordedCalls(
            params,
            matcher.substitute(represent)
        )
    }

    override fun markCallVerified(invocation: Invocation) {
        stub.markCallVerified(
            invocation.substitute(represent)
        )
    }

    override fun allRecordedCalls() = stub.allRecordedCalls()
        .map {
            it.substitute(revertRepresentation)
        }

    override fun allRecordedCalls(method: MethodDescription) =
        stub.allRecordedCalls(method)
            .map {
                it.substitute(revertRepresentation)
            }

    override fun verifiedCalls() = stub.verifiedCalls()
        .map {
            it.substitute(revertRepresentation)
        }

    override fun matcherUsages() = stub.matcherUsages()

    override fun clear(options: MockKGateway.ClearOptions) =
        stub.clear(options)

    override fun handleInvocation(
        self: Any,
        method: MethodDescription,
        originalCall: () -> Any?,
        args: Array<out Any?>,
        fieldValueProvider: BackingFieldValueProvider
    ) = stub.handleInvocation(
        self,
        method,
        originalCall,
        args,
        fieldValueProvider
    )

    override fun toStr() = stub.toStr()
    override fun stdObjectAnswer(invocation: Invocation) = stub.stdObjectAnswer(invocation.substitute(represent))
    override fun dispose() = stub.dispose()
}
