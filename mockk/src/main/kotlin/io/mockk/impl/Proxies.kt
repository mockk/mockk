package io.mockk.impl

import io.mockk.*
import io.mockk.MockKGateway.Stub
import io.mockk.external.logger
import java.lang.reflect.Method
import java.util.Collections.synchronizedList
import java.util.Collections.synchronizedMap
import kotlin.reflect.KClass


private data class InvocationAnswer(val matcher: InvocationMatcher, val answer: Answer<*>)

internal open class MockKStub(override val type: KClass<*>,
                              override val name: String) : Stub {

    private val answers = synchronizedList(mutableListOf<InvocationAnswer>())
    private val childs = synchronizedMap(hashMapOf<InvocationMatcher, Any>())
    private val recordedCalls = synchronizedList(mutableListOf<Invocation>())

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
            captureAnswer(matcher, invocation)

            val call = Call(invocation.method.returnType,
                    invocation,
                    matcher, false)

            answer.answer(call)
        }
    }

    private fun captureAnswer(invocationMatcher: InvocationMatcher, invocation: Invocation) {
        repeat(invocationMatcher.args.size) {
            val argMatcher = invocationMatcher.args[it]
            if (argMatcher is CapturingMatcher) {
                argMatcher.capture(invocation.args[it])
            }
        }
    }

    protected open fun defaultAnswer(invocation: Invocation): Any? {
        throw MockKException("no answer found for: $invocation")
    }

    override fun recordCall(invocation: Invocation) {
        recordedCalls.add(invocation)
    }

    override fun allRecordedCalls(): List<Invocation> {
        synchronized(recordedCalls) {
            return recordedCalls.toList()
        }
    }

    override fun toString() = "mockk<${type.simpleName}>(${this.name})"

    override fun childMockK(call: Call): Any? {
        return synchronized(childs) {
            childs.java6ComputeIfAbsent(call.matcher) {
                MockKGateway.implementation().mockFactory.mockk(
                        call.retType,
                        childName(this.name),
                        moreInterfaces = arrayOf())
            }
        }
    }

    private fun childName(name: String): String {
        val result = childOfRegex.matchEntire(name)
        return if (result != null) {
            val group = result.groupValues[2]
            val childN = if (group.isEmpty()) 1 else Integer.parseInt(group)
            "child^" + (childN + 1) + " of " + result.groupValues[3]
        } else {
            "child of " + name
        }
    }

    override fun handleInvocation(self: Any,
                                  thisMethod: MethodDescription,
                                  proceed: () -> Any?,
                                  args: Array<out Any?>): Any? {

        if (thisMethod.isToString()) {
            return toString()
        } else if (thisMethod.isHashCode()) {
            return System.identityHashCode(self)
        } else if (thisMethod.isEquals()) {
            return self === args[0]
        }

        val invocation = Invocation(
                self,
                thisMethod,
                args.toList(),
                System.nanoTime(),
                proceed)

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
    }
}



internal class SpyKStub<T : Any>(cls: KClass<T>, name: String) : MockKStub(cls, name) {
    override fun defaultAnswer(invocation: Invocation): Any? {
        if (invocation.realCall == null) {
            throw MockKException("no super method for: ${invocation.method}")
        }
        val realCall = invocation.realCall as () -> Any?
        return realCall()
    }

    override fun toString(): String = "spyk<" + type.simpleName + ">($name)"
}

internal fun MethodDescription.invoke(self: Any, vararg args: Any?) =
        (langDependentRef as Method).invoke(self, *args)

internal fun Method.toDescription() =
        MethodDescription(name, returnType.kotlin, declaringClass.kotlin, parameterTypes.map { it.kotlin }, this)

private fun MethodDescription.isToString() = name == "toString" && paramTypes.isEmpty()

private fun MethodDescription.isHashCode() = name == "hashCode" && paramTypes.isEmpty()

private fun MethodDescription.isEquals() = name == "equals" && paramTypes.size == 1 && paramTypes[0] == Any::class
