package io.mockk.impl.stub

import io.mockk.*
import kotlin.reflect.KClass

class ConstructorStub(
    val mock: Any,
    val representativeMock: Any,
    val stub: Stub,
    val recordPrivateCalls: Boolean
) : Stub {
    private val represent = mapOf(mock to representativeMock)
    private val revertRepresentatition = mapOf(representativeMock to mock)

    override val name: String
        get() = stub.name

    override val type: KClass<*>
        get() = stub.type

    override fun addAnswer(matcher: InvocationMatcher, answer: Answer<*>) =
        stub.addAnswer(matcher.substitute(represent), answer)

    override fun answer(invocation: Invocation) = stub.answer(
        invocation.substitute(represent)
    ).internalSubstitute(revertRepresentatition)

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

    override fun allRecordedCalls() = stub.allRecordedCalls()
        .map {
            it.substitute(revertRepresentatition)
        }

    override fun clear(answers: Boolean, calls: Boolean, childMocks: Boolean) =
        stub.clear(answers, calls, childMocks)

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