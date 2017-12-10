package io.mockk.impl.stub

import io.mockk.*
import io.mockk.impl.InternalPlatform
import kotlin.reflect.KClass
import io.mockk.impl.InternalPlatform.customComputeIfAbsent
import kotlin.math.sign

open class MockKStub(override val type: KClass<*>,
                     override val name: String,
                     val answerGenerator: AnswerGenerator?) : Stub {

    private val answers = InternalPlatform.synchronizedMutableList<InvocationAnswer>()
    private val childs = InternalPlatform.synchronizedMutableMap<InvocationMatcher, Any>()
    private val recordedCalls = InternalPlatform.synchronizedMutableList<Invocation>()

    lateinit var hashCodeStr: String

    override fun addAnswer(matcher: InvocationMatcher, answer: Answer<*>) {
        answers.add(InvocationAnswer(matcher, answer))
    }

    override fun answer(invocation: Invocation): Any? {
        val invocationAndMatcher = synchronized(answers) {
            answers
                    .reversed()
                    .firstOrNull { it.matcher.match(invocation) }
                    ?: return defaultAnswer(invocation)
        }

        return with(invocationAndMatcher) {
            matcher.captureAnswer(invocation)

            val call = MatchedCall(invocation.method.returnType,
                    invocation,
                    matcher, false)

            answer.answer(call)
        }
    }


    protected inline fun stdObjectFunctions(self: Any,
                                            method: MethodDescription,
                                            args: List<Any?>,
                                            otherwise: () -> Any?): Any? {
        if (method.isToString()) {
            return toStr()
        } else if (method.isHashCode()) {
            return InternalPlatformDsl.identityHashCode(self)
        } else if (method.isEquals()) {
            return self === args[0]
        } else {
            return otherwise()
        }
    }

    protected open fun defaultAnswer(invocation: Invocation): Any? {
        return stdObjectFunctions(invocation.self, invocation.method, invocation.args) {
            val gen = answerGenerator
            if (gen == null) {
                throw MockKException("no answer found for: $invocation")
            } else {
                return gen(invocation.method.returnType)
            }
        }
    }

    override fun recordCall(invocation: Invocation) {
        recordedCalls.add(invocation)
    }

    override fun allRecordedCalls(): List<Invocation> {
        synchronized(recordedCalls) {
            return recordedCalls.toList()
        }
    }

    override fun toStr() = "mockk<${type.simpleName}>(${this.name})#$hashCodeStr"

    override fun childMockK(call: MatchedCall): Any? {
        return synchronized(childs) {
            childs.customComputeIfAbsent(call.matcher) {
                MockKGateway.implementation().mockFactory.mockk(
                        call.retType,
                        childName(this.name),
                        moreInterfaces = arrayOf(),
                        relaxed = answerGenerator != null)
            }
        }
    }

    private fun childName(name: String): String {
        val result = childOfRegex.matchEntire(name)
        return if (result != null) {
            val group = result.groupValues[2]
            val childN = if (group.isEmpty()) 1 else group.toInt()
            "child^" + (childN + 1) + " of " + result.groupValues[3]
        } else {
            "child of " + name
        }
    }

    override fun handleInvocation(self: Any,
                                  method: MethodDescription,
                                  originalCall: () -> Any?,
                                  args: Array<out Any?>): Any? {
        val originalPlusToString = {
            if (method.isToString()) {
                toStr()
            } else {
                originalCall()
            }
        }

        val invocation = Invocation(
                self,
                toStr(),
                method,
                args.toList(),
                InternalPlatform.time(),
                originalPlusToString)

        return MockKGateway.implementation().callRecorder.call(invocation)
    }

    override fun clear(answers: Boolean, calls: Boolean, childMocks: Boolean) {
        if (answers) {
            this.answers.clear()
        }
        if (calls) {
            this.recordedCalls.clear()
        }
        if (childMocks) {
            this.childs.clear()
        }
    }

    companion object {
        val childOfRegex = Regex("child(\\^(\\d+))? of (.+)")

        fun MethodDescription.isToString() = name == "toString" && paramTypes.isEmpty()
        fun MethodDescription.isHashCode() = name == "hashCode" && paramTypes.isEmpty()
        fun MethodDescription.isEquals() = name == "equals" && paramTypes.size == 1 && paramTypes[0] == Any::class

    }

    private data class InvocationAnswer(val matcher: InvocationMatcher, val answer: Answer<*>)
}

typealias AnswerGenerator = (KClass<*>) -> Any?
