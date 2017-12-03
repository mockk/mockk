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

    inner abstract class State {
        open fun call(invocation: Invocation): Any? = cancelAndThrowBadRecordingState()
        open fun startStubbing(): State = cancelAndThrowBadRecordingState()
        open fun startVerification(): State = cancelAndThrowBadRecordingState()
        open fun catchArgs(round: Int, n: Int): Unit = cancelAndThrowBadRecordingState()
        open fun answer(answer: Answer<*>): Unit = cancelAndThrowBadRecordingState()
        open fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T = cancelAndThrowBadRecordingState()
        open fun doneVerification(): State = cancelAndThrowBadRecordingState()
        open fun nCalls(): Int = cancelAndThrowBadRecordingState()
        open fun estimateCallRounds(): Int = cancelAndThrowBadRecordingState()
    }

    inner class AnsweringState : State() {
        override fun call(invocation: Invocation): Any? {
            val stub = stubRepo.stubFor(invocation.self)
            stub.recordCall(invocation.copy(originalCall = { null }))
            val answer = stub.answer(invocation)
            log.debug { "Recorded call: $invocation, answer: ${answerToString(answer)}" }
            return answer
        }

        override fun startStubbing() = StubbingState()
        override fun startVerification() = VerifyingState()

        private fun answerToString(answer: Any?) = stubRepo[answer]?.toStr() ?: answer.toString()
    }

    inner abstract class RecordingState : State() {
        val childMocks = mutableListOf<Ref>()
        val temporaryMocks = mutableMapOf<KClass<*>, Any>()

        private val signedCalls = mutableListOf<SignedCall>()
        private val callRounds = mutableListOf<CallRound>()
        private val matchers = mutableListOf<Matcher<*>>()
        private val signatures = mutableListOf<Any>()

        override fun catchArgs(round: Int, n: Int) {
            if (state !is RecordingState) {
                cancelAndThrowBadRecordingState()
            }
            if (round > 0) {
                callRounds.add(CallRound(signedCalls.toList()))
            }

            signedCalls.clear()
            childHinter = ChildHinter()

            if (round == n) {
                try {
                    signMatchers()
                    mockRealChilds()
                } finally {
                    callRounds.clear()
                }
            }
        }

        private fun signMatchers() {
            calls.clear()
            val detector = SignatureMatcherDetector(callRounds, childMocks)
            calls.addAll(detector.detect())
        }

        override fun call(invocation: Invocation): Any? {
            if (childMocks.any { mock -> invocation.args.any { it === mock } }) {
                throw MockKException("Passing child mocks to arguments is prohibited")
            }
            val retType = childHinter.nextChildType { invocation.method.returnType }

            signedCalls.add(SignedCall(retType, invocation, matchers.toList(), signatures.toList()))
            matchers.clear()
            signatures.clear()

            return anyValueGenerator.anyValue(retType) {
                try {
                    val mock = temporaryMocks[retType]
                    if (mock != null) {
                        return@anyValue mock
                    }

                    val child = mockFactory.childMock(retType)

                    childMocks.add(InternalPlatform.ref(child))

                    temporaryMocks[retType] = child

                    child
                } catch (ex: MockKException) {
                    log.trace(ex) { "Returning 'null' for a final class assuming it is last in a call chain" }
                    null
                }
            }
        }

        fun mockRealChilds() {
            var newSelf: Any? = null
            val newCalls = mutableListOf<MatchedCall>()

            for ((idx, ic) in calls.withIndex()) {
                val lastCall = idx == calls.size - 1

                val invocation = ic.invocation

                if (!ic.chained) {
                    newSelf = invocation.self
                }

                val newInvocation = ic.invocation.copy(self = newSelf!!)
                val newMatcher = ic.matcher.copy(self = newSelf)
                val newCall = ic.copy(invocation = newInvocation, matcher = newMatcher)

                newCalls.add(newCall)

                if (!lastCall && calls[idx + 1].chained) {

                    val args = newCall.matcher.args.map {
                        when (it) {
                            is EquivalentMatcher -> it.equivalent()
                            else -> it
                        }
                    }
                    val matcher = newCall.matcher.copy(args = args)
                    val equivalentCall = newCall.copy(matcher = matcher)

                    log.trace { "Child search key: $matcher" }

                    newSelf = stubRepo.stubFor(newSelf).childMockK(equivalentCall)
                }
            }

            calls.clear()
            calls.addAll(newCalls)

            log.trace { "Mocked childs" }
        }

        override fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T {
            matchers.add(matcher)
            val signatureValue = signatureValueGenerator.signatureValue(cls) {
                instantiator.instantiate(cls)
            }
            signatures.add(InternalPlatform.packRef(signatureValue)!!)
            return signatureValue
        }

        override fun nCalls(): Int = signedCalls.size

        /**
         * Main idea is to have enough random information
         * to create signature for the argument.
         *
         * Max 40 calls looks like reasonable compromise
         */
        override fun estimateCallRounds(): Int {
            return signedCalls
                    .flatMap { it.invocation.args }
                    .filterNotNull()
                    .map {
                        when (it::class) {
                            Boolean::class -> 40
                            Byte::class -> 8
                            Char::class -> 4
                            Short::class -> 4
                            Int::class -> 2
                            Float::class -> 2
                            else -> 1
                        }
                    }
                    .max() ?: 1
        }
    }

    inner class VerifyingState : RecordingState() {
        override fun doneVerification() = AnsweringState()
    }

    inner class StubbingState : RecordingState() {
        override fun catchArgs(round: Int, n: Int) {
            super.catchArgs(round, n)
            if (round == n) {
                state = StubbingAwaitingAnswerState()
            }
        }
    }

    inner class StubbingAwaitingAnswerState : State() {
        override fun answer(answer: Answer<*>) {
            for ((idx, ic) in calls.withIndex()) {
                val lastCall = idx == calls.size - 1

                val ans = if (lastCall) {
                    answer
                } else {
                    ConstantAnswer(calls[idx + 1].invocation.self)
                }

                stubRepo.stubFor(ic.invocation.self).addAnswer(ic.matcher, ans)
            }

            calls.clear()

            log.trace { "Done stubbing" }

            state = AnsweringState()
        }
    }


    override val calls = mutableListOf<MatchedCall>()
    private var state: State = AnsweringState()
    private var childHinter = ChildHinter()

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

    override fun cancel() {
        calls.clear()
        childHinter = ChildHinter()
        state = AnsweringState()
    }


    private fun cancelAndThrowBadRecordingState(): Nothing {
        cancel()
        throw MockKException("Bad recording sequence. State: ${state::class}")
    }

    companion object {
        val log = Logger<CallRecorderImpl>()
    }
}

