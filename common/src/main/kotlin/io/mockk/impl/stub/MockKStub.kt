package io.mockk.impl.stub

import io.mockk.*
import io.mockk.impl.InternalPlatform
import io.mockk.impl.InternalPlatform.customComputeIfAbsent
import kotlin.reflect.KClass

open class MockKStub(
    override val type: KClass<*>,
    override val name: String,
    val relaxed: Boolean = false,
    val gatewayAccess: StubGatewayAccess,
    val recordPrivateCalls: Boolean
) : Stub {
    private val answers = InternalPlatform.synchronizedMutableList<InvocationAnswer>()
    private val childs = InternalPlatform.synchronizedMutableMap<InvocationMatcher, Any>()
    private val recordedCalls = InternalPlatform.synchronizedMutableList<Invocation>()

    lateinit var hashCodeStr: String

    var disposeRoutine: () -> Unit = {}

    override fun addAnswer(matcher: InvocationMatcher, answer: Answer<*>): AdditionalAnswerOpportunity {
        val invocationAnswer = InvocationAnswer(matcher, answer)
        answers.add(invocationAnswer)

        return AdditionalAnswerOpportunity({
            synchronized(answers) {
                invocationAnswer.answer
            }
        }, {
            synchronized(answers) {
                invocationAnswer.answer = it
            }
        })
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

            val call = Call(
                invocation.method.returnType,
                invocation,
                matcher,
                invocation.fieldValueProvider
            )

            answer.answer(call)
        }
    }


    protected inline fun stdObjectFunctions(
        self: Any,
        method: MethodDescription,
        args: List<Any?>,
        otherwise: () -> Any?
    ): Any? {
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

    override fun stdObjectAnswer(invocation: Invocation): Any? {
        return stdObjectFunctions(invocation.self, invocation.method, invocation.args) {
            throw MockKException("No other calls allowed in stdObjectAnswer than equals/hashCode/toString")
        }
    }

    protected open fun defaultAnswer(invocation: Invocation): Any? {
        return stdObjectFunctions(invocation.self, invocation.method, invocation.args) {
            if (relaxed) {
                return gatewayAccess.anyValueGenerator.anyValue(invocation.method.returnType) {
                    childMockK(invocation.allEqMatcher(), invocation.method.returnType)
                }
            } else {
                throw MockKException("no answer found for: $invocation")
            }
        }
    }

    override fun recordCall(invocation: Invocation) {
        val record = if (recordPrivateCalls)
            true
        else
            !invocation.method.privateCall

        if (record) {
            recordedCalls.add(invocation)
        }
    }

    override fun allRecordedCalls(): List<Invocation> {
        synchronized(recordedCalls) {
            return recordedCalls.toList()
        }
    }

    override fun toStr() = "${type.simpleName}($name)"

    override fun childMockK(matcher: InvocationMatcher, childType: KClass<*>): Any {
        return synchronized(childs) {
            gatewayAccess.safeLog.exec {
                childs.customComputeIfAbsent(matcher) {
                    gatewayAccess.mockFactory!!.mockk(
                        childType,
                        childName(this.name),
                        moreInterfaces = arrayOf(),
                        relaxed = relaxed
                    )
                }
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

    override fun handleInvocation(
        self: Any,
        method: MethodDescription,
        originalCall: () -> Any?,
        args: Array<out Any?>,
        fieldValueProvider: BackingFieldValueProvider
    ): Any? {
        val originalPlusToString = {
            if (method.isToString()) {
                toStr()
            } else {
                originalCall()
            }
        }


        fun List<StackElement>.cutMockKCallProxyCall(): List<StackElement> {
            fun List<StackElement>.search(cls: String, mtd: String): Int? {
                return indexOfFirst {
                    it.className == cls && it.methodName == mtd
                }.let { if (it == -1) null else it }
            }

            val idx = search("io.mockk.proxy.MockKCallProxy", "call")
                    ?: search("io.mockk.proxy.MockKProxyInterceptor", "intercept")
                    ?: search("io.mockk.proxy.MockKProxyInterceptor", "interceptNoSuper")
                    ?: return this

            return this.drop(idx + 1)
        }

        fun List<StackElement>.unmangleByteBuddy(): List<StackElement> {
            return map {
                val idx = it.className.indexOf("\$ByteBuddy\$")
                if (idx == -1)
                    it
                else
                    it.copy(className = it.className.substring(0, idx) + "(BB)")
            }
        }

        val stackTraceHolder = InternalPlatform.captureStackTrace()

        val invocation = Invocation(
            self,
            this,
            method,
            args.toList(),
            InternalPlatform.time(),
            {
                stackTraceHolder()
                    .cutMockKCallProxyCall()
                    .unmangleByteBuddy()
            },
            originalPlusToString,
            fieldValueProvider
        )

        return gatewayAccess.callRecorder().call(invocation)
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

    private data class InvocationAnswer(val matcher: InvocationMatcher, var answer: Answer<*>)

    protected fun Invocation.allEqMatcher() =
        InvocationMatcher(
            self,
            method,
            args.map {
                if (it == null)
                    NullCheckMatcher<Any>()
                else
                    EqMatcher(it)
            }, false
        )

    override fun dispose() {
        clear(true, true, true)
        disposeRoutine.invoke()
    }
}
