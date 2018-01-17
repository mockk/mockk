package io.mockk.impl.recording

import io.mockk.EqMatcher
import io.mockk.EquivalentMatcher
import io.mockk.RecordedCall
import io.mockk.impl.InternalPlatform
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeLog
import io.mockk.impl.stub.StubRepository

class PermanentMocker(
    val stubRepo: StubRepository,
    val safeLog: SafeLog
) {

    val log = safeLog(Logger<PermanentMocker>())

    val permanentMocks = InternalPlatform.identityMap<Any, Any>()
    val callRef = InternalPlatform.weakMap<Any, RecordedCall>()

    fun mock(calls: List<RecordedCall>): List<RecordedCall> {
        val result = mutableListOf<RecordedCall>()
        for (call in calls) {
            val permanentCall = permamentize(call)
            result.add(permanentCall)
        }

        val callTree = safeLog.exec { describeCallTree(result) }
        if (callTree.size == 1) {
            log.trace { "Mocked permanently: " + callTree[0] }
        } else {
            log.trace { "Mocked permanently:\n" + callTree.joinToString(", ") }
        }

        return result
    }

    private fun permamentize(call: RecordedCall): RecordedCall {
        val newCall = makeCallPermanent(call)

        val retValue = call.retValue
        if (call.isRetValueMock && retValue != null) {
            val equivalentCall = makeEquivalent(newCall)

            log.trace { "Child search key: ${equivalentCall.matcher}" }

            val childMock = stubRepo.stubFor(newCall.matcher.self)
                .childMockK(equivalentCall.matcher, equivalentCall.retType)

            val newNewCall = newCall.copy(retValue = childMock)

            permanentMocks[retValue] = childMock
            callRef[retValue] = newNewCall

            return newNewCall
        }

        return newCall
    }

    private fun makeEquivalent(newCall: RecordedCall): RecordedCall {
        val equivalentArgs = newCall.matcher.args.map {
            when (it) {
                is EquivalentMatcher -> it.equivalent()
                else -> it
            }
        }

        val equivalentIM = newCall.matcher.copy(args = equivalentArgs)
        return newCall.copy(matcher = equivalentIM)
    }

    private fun makeCallPermanent(call: RecordedCall): RecordedCall {
        val selfChain = callRef[call.matcher.self]
        val argChains = call.matcher.args
            .map {
                when (it) {
                    is EqMatcher -> callRef[it.value] ?: it
                    else -> it
                }
            }

        val newSelf = permanentMocks[call.matcher.self] ?: call.matcher.self
        val newArgs = call.matcher.args.map { it.substitute(permanentMocks) }
        val newMatcher = call.matcher.copy(self = newSelf, args = newArgs)
        return call.copy(
            matcher = newMatcher,
            selfChain = selfChain,
            argChains = argChains
        )
    }

    private fun describeCallTree(calls: MutableList<RecordedCall>): List<String> {
        val callTree = linkedMapOf<RecordedCall, String>()
        val usedCalls = hashSetOf<RecordedCall>()

        for (call in calls) {
            callTree[call] = formatCall(
                call,
                callTree,
                usedCalls
            )
        }

        return calls.filter {
            it !in usedCalls
        }.map {
                callTree[it] ?: "<bad call>"
            }
    }

    private fun formatCall(
        call: RecordedCall,
        tree: Map<RecordedCall, String>,
        usedCalls: MutableSet<RecordedCall>
    ): String {
        val methodName = call.matcher.method.name
        val args = call.argChains!!.map {
            when (it) {
                is RecordedCall -> {
                    usedCalls.add(it)
                    tree[it] ?: "<bad link>"
                }
                else -> it.toString()
            }
        }

        val selfChain = call.selfChain
        val prefix = if (selfChain != null) {
            usedCalls.add(selfChain)
            (tree[selfChain] ?: "<bad link>") + "."
        } else {
            call.matcher.self.toString() + "."
        }

        if (methodName.startsWith("get") &&
            methodName.length > 3 &&
            args.isEmpty()
        ) {
            return prefix +
                    methodName[3].toLowerCase() +
                    methodName.substring(4)
        }

        return prefix + methodName + "(" + args.joinToString(", ") + ")"
    }

}