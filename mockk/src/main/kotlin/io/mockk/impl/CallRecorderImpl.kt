package io.mockk.impl

import io.mockk.*
import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKGateway.MockFactory
import kotlin.reflect.KClass

internal class CallRecorderImpl(val stubRepo: StubRepository,
                                val instantiator: Instantiator,
                                val signatureValueGenerator: SignatureValueGenerator,
                                val mockFactory: MockFactory,
                                val anyValueGenerator: AnyValueGenerator) : CallRecorder {

    override val calls = mutableListOf<MatchedCall>()
    internal var state: CallRecorderState = AnsweringCallRecorderState(this)
    internal var childHinter = ChildHinter()

    class ChildHinter {
        private var childTypes = mutableMapOf<Int, KClass<*>>()

        fun nextChildType(defaultReturnType: () -> KClass<*>): KClass<*> {
            val type = childTypes[1]
            shift()
            return type ?: defaultReturnType()
        }

        private fun shift() {
            childTypes = childTypes
                    .mapKeys { (k, _) -> k - 1 }
                    .filter { (k, _) -> k > 0 }
                    .toMutableMap()
        }

        fun hint(n: Int, cls: KClass<*>) {
            childTypes[n] = cls
        }
    }

    override fun startStubbing() {
        state = state.startStubbing()
        log.trace { "Starting stubbing" }
    }

    override fun startVerification() {
        state = state.startVerification()
        log.trace { "Starting verification" }
    }

    override fun catchArgs(round: Int, n: Int) = state.catchArgs(round, n)
    override fun nCalls() = state.nCalls()
    override fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T = state.matcher(matcher, cls)
    override fun call(invocation: Invocation) = state.call(invocation)
    override fun answer(answer: Answer<*>) = state.answer(answer)
    override fun hintNextReturnType(cls: KClass<*>, n: Int) = childHinter.hint(n, cls)
    override fun estimateCallRounds(): Int = state.estimateCallRounds()

    override fun doneVerification() {
        state = state.doneVerification()
    }

    override fun reset() {
        calls.clear()
        childHinter = ChildHinter()
        state = AnsweringCallRecorderState(this)
    }

    companion object {
        val log = Logger<CallRecorderImpl>()
    }
}

