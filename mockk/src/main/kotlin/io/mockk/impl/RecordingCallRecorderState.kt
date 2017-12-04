package io.mockk.impl

import io.mockk.*
import kotlin.reflect.KClass

internal abstract class RecordingCallRecorderState(recorder: CallRecorderImpl) : CallRecorderState(recorder) {
    val childMocks = mutableListOf<Ref>()
    val temporaryMocks = mutableMapOf<KClass<*>, Any>()

    private val signedCalls = mutableListOf<SignedCall>()
    private val callRounds = mutableListOf<CallRound>()
    private val matchers = mutableListOf<Matcher<*>>()
    private val signatures = mutableListOf<Any>()

    override fun catchArgs(round: Int, n: Int) {
        if (round > 0) {
            callRounds.add(CallRound(signedCalls.toList()))
        }

        signedCalls.clear()
        recorder.childHinter = ChildHinter()

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
        recorder.calls.clear()
        val detector = SignatureMatcherDetector(callRounds, childMocks)
        recorder.calls.addAll(detector.detect())
    }

    override fun call(invocation: Invocation): Any? {
        if (childMocks.any { mock -> invocation.args.any { it === mock } }) {
            throw MockKException("Passing child mocks to arguments is prohibited")
        }
        val retType = recorder.childHinter.nextChildType { invocation.method.returnType }

        signedCalls.add(SignedCall(retType, invocation, matchers.toList(), signatures.toList()))
        matchers.clear()
        signatures.clear()

        return recorder.anyValueGenerator.anyValue(retType) {
            try {
                val mock = temporaryMocks[retType]
                if (mock != null) {
                    return@anyValue mock
                }

                val child = recorder.mockFactory.childMock(retType)

                childMocks.add(InternalPlatform.ref(child))

                temporaryMocks[retType] = child

                child
            } catch (ex: MockKException) {
                CallRecorderImpl.log.trace(ex) { "Returning 'null' for a final class assuming it is last in a call chain" }
                null
            }
        }
    }

    fun mockRealChilds() {
        var newSelf: Any? = null
        val newCalls = mutableListOf<MatchedCall>()

        for ((idx, ic) in recorder.calls.withIndex()) {
            val lastCall = idx == recorder.calls.size - 1

            val invocation = ic.invocation

            if (!ic.chained) {
                newSelf = invocation.self
            }

            val newInvocation = ic.invocation.copy(self = newSelf!!)
            val newMatcher = ic.matcher.copy(self = newSelf)
            val newCall = ic.copy(invocation = newInvocation, matcher = newMatcher)

            newCalls.add(newCall)

            if (!lastCall && recorder.calls[idx + 1].chained) {

                val args = newCall.matcher.args.map {
                    when (it) {
                        is EquivalentMatcher -> it.equivalent()
                        else -> it
                    }
                }
                val matcher = newCall.matcher.copy(args = args)
                val equivalentCall = newCall.copy(matcher = matcher)

                CallRecorderImpl.log.trace { "Child search key: $matcher" }

                newSelf = recorder.stubRepo.stubFor(newSelf).childMockK(equivalentCall)
            }
        }

        recorder.calls.clear()
        recorder.calls.addAll(newCalls)

        CallRecorderImpl.log.trace { "Mocked childs" }
    }

    override fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T {
        matchers.add(matcher)
        val signatureValue = recorder.signatureValueGenerator.signatureValue(cls) {
            recorder.instantiator.instantiate(cls)
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