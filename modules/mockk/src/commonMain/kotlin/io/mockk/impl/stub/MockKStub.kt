package io.mockk.impl.stub

import io.mockk.*
import io.mockk.impl.InternalPlatform
import io.mockk.impl.InternalPlatform.customComputeIfAbsent
import io.mockk.impl.log.Logger
import kotlin.reflect.KClass

open class MockKStub(
    override val type: KClass<*>,
    override val name: String,
    val relaxed: Boolean = false,
    val relaxUnitFun: Boolean = false,
    val gatewayAccess: StubGatewayAccess,
    val recordPrivateCalls: Boolean,
    val mockType: MockType
) : Stub {
    val log = gatewayAccess.safeToString(Logger<MockKStub>())

    override val threadId: Long = Thread.currentThread().id

    private val answers = InternalPlatform.synchronizedMutableList<InvocationAnswer>()
    private val childs = InternalPlatform.synchronizedMutableMap<InvocationMatcher, Any>()
    private val recordedCalls = InternalPlatform.synchronizedMutableList<Invocation>()
    private val recordedCallsByMethod =
        InternalPlatform.synchronizedMutableMap<MethodDescription, MutableList<Invocation>>()
    private val exclusions = InternalPlatform.synchronizedMutableList<InvocationMatcher>()
    private val verifiedCalls = InternalPlatform.synchronizedMutableList<Invocation>()

    lateinit var hashCodeStr: String

    var disposeRoutine: () -> Unit = {}

    override fun addAnswer(matcher: InvocationMatcher, answer: Answer<*>) {
        val invocationAnswer = InvocationAnswer(matcher, answer, 0)
        answers.add(invocationAnswer)
    }

    override fun answer(invocation: Invocation): Any? {
        val invocationAndMatcher = InternalPlatform.synchronized(answers) {
            answers
                .findLast { it.matcher.match(invocation) }
                ?.also { it.usageCount++ }
        } ?: return defaultAnswer(invocation)

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
        return when {
            method.isToString() -> toStr()
            method.isHashCode() -> InternalPlatformDsl.identityHashCode(self)
            method.isEquals() -> self === args[0]
            else -> otherwise()
        }
    }

    override fun stdObjectAnswer(invocation: Invocation): Any? {
        return stdObjectFunctions(invocation.self, invocation.method, invocation.args) {
            throw MockKException("No other calls allowed in stdObjectAnswer than equals/hashCode/toString")
        }
    }

    protected open fun defaultAnswer(invocation: Invocation): Any? =
        stdObjectFunctions(invocation.self, invocation.method, invocation.args) {
            if (shouldRelax(invocation)) {
                if (invocation.method.returnsUnit) return Unit
                return gatewayAccess.anyValueGenerator().anyValue(
                    invocation.method.returnType,
                    invocation.method.returnTypeNullable
                ) {
                    childMockK(invocation.allEqMatcher(), invocation.method.returnType)
                }
            } else {
                val configuredAnswers = answers.map { it.matcher.toString() }.joinToString(separator = "\n") { it }
                throw MockKException("no answer found for ${gatewayAccess.safeToString.exec { invocation.toString() }}" +
                        " among the configured answers: ($configuredAnswers)")
            }
        }

    private fun shouldRelax(invocation: Invocation) = when {
        relaxed -> true
        relaxUnitFun &&
                invocation.method.returnsUnit -> true
        else -> false
    }

    override fun recordCall(invocation: Invocation) {
        val record = when {
            checkExcluded(invocation) -> {
                log.debug { "Call excluded: $invocation" }
                false
            }
            recordPrivateCalls -> true
            else -> !invocation.method.privateCall
        }

        if (record) {
            recordedCalls.add(invocation)

            InternalPlatform.synchronized(recordedCallsByMethod) {
                recordedCallsByMethod.getOrPut(invocation.method) { mutableListOf() }
                    .add(invocation)
            }

            gatewayAccess.stubRepository.notifyCallRecorded(this)
        }
    }

    private fun checkExcluded(invocation: Invocation) = InternalPlatform.synchronized(exclusions) {
        exclusions.any { it.match(invocation) }
    }

    override fun allRecordedCalls(): List<Invocation> {
        InternalPlatform.synchronized(recordedCalls) {
            return recordedCalls.toList()
        }
    }

    override fun allRecordedCalls(method: MethodDescription): List<Invocation> {
        InternalPlatform.synchronized(recordedCallsByMethod) {
            return recordedCallsByMethod[method]?.toList() ?: listOf()
        }
    }

    override fun excludeRecordedCalls(
        params: MockKGateway.ExclusionParameters,
        matcher: InvocationMatcher
    ) {
        exclusions.add(matcher)

        if (params.current) {
            InternalPlatform.synchronized(recordedCalls) {
                val callsToExclude = recordedCalls
                    .filter(matcher::match)

                if (callsToExclude.isNotEmpty()) {
                    log.debug {
                        "Calls excluded: " + callsToExclude.joinToString(", ")
                    }
                }

                callsToExclude
                    .forEach { recordedCalls.remove(it) }

                InternalPlatform.synchronized(recordedCallsByMethod) {

                    recordedCallsByMethod[matcher.method]?.apply {
                        filter(matcher::match)
                            .forEach { remove(it) }
                    }
                }

                InternalPlatform.synchronized(verifiedCalls) {
                    verifiedCalls
                        .filter(matcher::match)
                        .forEach { verifiedCalls.remove(it) }
                }
            }
        }
    }

    override fun markCallVerified(invocation: Invocation) {
        verifiedCalls.add(invocation)
    }

    override fun verifiedCalls(): List<Invocation> {
        InternalPlatform.synchronized(verifiedCalls) {
            return verifiedCalls.toList()
        }
    }

    override fun matcherUsages(): Map<InvocationMatcher, Int> =
        InternalPlatform.synchronized(answers) {
            answers.associate { it.matcher to it.usageCount }
        }

    override fun toStr() = "${type.simpleName}($name)"

    override fun childMockK(matcher: InvocationMatcher, childType: KClass<*>): Any {
        return InternalPlatform.synchronized(childs) {
            gatewayAccess.safeToString.exec {
                childs.customComputeIfAbsent(matcher) {
                    gatewayAccess.mockFactory!!.mockk(
                        childType,
                        childName(this.name),
                        moreInterfaces = arrayOf(),
                        relaxed = relaxed,
                        relaxUnitFun = relaxUnitFun
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
            "child of $name"
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
            fun search(cls: String, mtd: String): Int? {
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
            },
            originalPlusToString,
            fieldValueProvider
        )

        return gatewayAccess.callRecorder().call(invocation)
    }

    override fun clear(options: MockKGateway.ClearOptions) {
        if (options.answers) {
            this.answers.clear()
        }
        if (options.recordedCalls) {
            this.recordedCalls.clear()
            this.recordedCallsByMethod.clear()
        }
        if (options.childMocks) {
            this.childs.clear()
        }
        if (options.verificationMarks) {
            this.verifiedCalls.clear()
        }
        if (options.exclusionRules) {
            this.exclusions.clear()
        }
    }

    companion object {
        val childOfRegex = Regex("child(\\^(\\d+))? of (.+)")
    }

    private data class InvocationAnswer(val matcher: InvocationMatcher, val answer: Answer<*>, var usageCount: Int)

    protected fun Invocation.allEqMatcher() =
        InvocationMatcher(
            self,
            method,
            args.map {
                if (it == null)
                    NullCheckMatcher()
                else
                    EqMatcher(it)
            }, false
        )

    override fun dispose() {
        clear(
            MockKGateway.ClearOptions(
                answers = true,
                recordedCalls = true,
                childMocks = true,
                verificationMarks = true,
                exclusionRules = true
            )
        )
        disposeRoutine.invoke()
    }
}
