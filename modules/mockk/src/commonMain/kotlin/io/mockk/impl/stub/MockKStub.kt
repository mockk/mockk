package io.mockk.impl.stub

import io.mockk.Answer
import io.mockk.BackingFieldValueProvider
import io.mockk.Call
import io.mockk.EqMatcher
import io.mockk.InternalPlatformDsl
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.Invocation
import io.mockk.InvocationMatcher
import io.mockk.MethodDescription
import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.NullCheckMatcher
import io.mockk.StackElement
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
    val mockType: MockType,
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
    private val unansweredCalls = InternalPlatform.synchronizedMutableList<Invocation>()

    lateinit var hashCodeStr: String

    var disposeRoutine: () -> Unit = {}

    override fun addAnswer(
        matcher: InvocationMatcher,
        answer: Answer<*>,
    ) {
        val invocationAnswer = InvocationAnswer(matcher, answer, 0)
        answers.add(invocationAnswer)
    }

    override fun answer(invocation: Invocation): Any? = answerWithRecordStep(invocation) {}

    override fun answerWithRecordStep(
        invocation: Invocation,
        recordCallStep: () -> Unit,
    ): Any? {
        val invocationAndMatcher =
            InternalPlatform.synchronized(answers) {
                answers
                    .findLast { it.matcher.match(invocation) }
                    ?.also { it.usageCount++ }
            }

        if (invocationAndMatcher == null) {
            recordCallStep()
            return defaultAnswer(invocation)
        }

        return with(invocationAndMatcher) {
            try {
                matcher.captureAnswer(invocation)

                // The call becomes visible to verifiers only after its capture slots are
                // filled: verify(timeout) polls the recorded-calls lists and is woken by the
                // recording, so it can never pass while the captured arguments are still
                // missing (issue #1169).
                recordCallStep()

                val call =
                    Call(
                        invocation.method.returnType,
                        invocation,
                        matcher,
                        invocation.fieldValueProvider,
                    )

                answer.answer(call)
            } catch (ex: Exception) {
                markCallUnanswered(invocation)
                throw ex
            }
        }
    }

    protected inline fun stdObjectFunctions(
        self: Any,
        method: MethodDescription,
        args: List<Any?>,
        otherwise: () -> Any?,
    ): Any? =
        when {
            method.isToString() -> toStr()
            method.isHashCode() -> InternalPlatformDsl.identityHashCode(self)
            method.isEquals() -> self === args[0]
            else -> otherwise()
        }

    override fun stdObjectAnswer(invocation: Invocation): Any? =
        stdObjectFunctions(invocation.self, invocation.method, invocation.args) {
            throw MockKException("No other calls allowed in stdObjectAnswer than equals/hashCode/toString")
        }

    protected open fun defaultAnswer(invocation: Invocation): Any? =
        stdObjectFunctions(invocation.self, invocation.method, invocation.args) {
            if (shouldRelax(invocation)) {
                if (invocation.method.returnsUnit) return Unit
                return gatewayAccess.anyValueGenerator().anyValue(
                    invocation.method.returnType,
                    invocation.method.returnTypeNullable,
                ) {
                    childMockK(invocation.allEqMatcher(), invocation.method.returnType)
                }
            } else {
                val signature = invocation.method.typedSignature()
                val configuredAnswers =
                    answers.joinToString(separator = "\n") { answer ->
                        val matcher = answer.matcher
                        gatewayAccess.safeToString.exec {
                            "${matcher.self}.${matcher.method.name}(${matcher.args.joinToString(
                                ", ",
                            )}) [${matcher.method.typedSignature()}]"
                        }
                    }
                markCallUnanswered(invocation)
                throw MockKException(
                    "no answer found for ${
                        gatewayAccess.safeToString.exec {
                            "${invocation.self}.${invocation.method.name}(${invocation.args.joinToString(
                                ", ",
                            ) { it.toStr() }}) [$signature]"
                        }
                    }" +
                        " among the configured answers: ($configuredAnswers)",
                )
            }
        }

    private fun shouldRelax(invocation: Invocation) =
        when {
            relaxed -> true
            relaxUnitFun &&
                invocation.method.returnsUnit -> true
            else -> false
        }

    override fun recordCall(invocation: Invocation) {
        val record =
            when {
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
                recordedCallsByMethod
                    .getOrPut(invocation.method) { mutableListOf() }
                    .add(invocation)
            }

            gatewayAccess.stubRepository.notifyCallRecorded(this)
        }
    }

    private fun checkExcluded(invocation: Invocation) =
        InternalPlatform.synchronized(exclusions) {
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

    /**
     * Marks [invocation] as reached-but-unanswered: it either found no stubbed answer
     * (and the mock is not relaxed, see [defaultAnswer]) or its matched answer threw
     * during execution (see [answer]). Purely informational: the ordered/sequence
     * verifiers use it to append a divergence hint to failure messages (issue #959),
     * it never affects matching or pass/fail decisions.
     */
    private fun markCallUnanswered(invocation: Invocation) {
        unansweredCalls.add(invocation)
    }

    internal fun allUnansweredCalls(): List<Invocation> {
        InternalPlatform.synchronized(unansweredCalls) {
            return unansweredCalls.toList()
        }
    }

    override fun excludeRecordedCalls(
        params: MockKGateway.ExclusionParameters,
        matcher: InvocationMatcher,
    ) {
        exclusions.add(matcher)

        if (params.current) {
            InternalPlatform.synchronized(recordedCalls) {
                val callsToExclude =
                    recordedCalls
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

                InternalPlatform.synchronized(unansweredCalls) {
                    unansweredCalls
                        .filter(matcher::match)
                        .forEach { unansweredCalls.remove(it) }
                }
            }
        }
    }

    override fun markCallVerified(invocation: Invocation) {
        verifiedCalls.add(invocation)
    }

    override fun markAnswerUsed(matcher: InvocationMatcher) {
        InternalPlatform.synchronized(answers) {
            answers.lastOrNull { it.matcher == matcher }?.let { it.usageCount++ }
        }
    }

    override fun markAnswerLenient(matcher: InvocationMatcher) {
        InternalPlatform.synchronized(answers) {
            answers.lastOrNull { it.matcher == matcher }?.lenient = true
        }
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

    override fun lenientMatchers(): Set<InvocationMatcher> =
        InternalPlatform.synchronized(answers) {
            answers
                .filter { it.lenient }
                .map { it.matcher }
                .toSet()
        }

    override fun toStr() = "${type.simpleName}($name)"

    override fun childMockK(
        matcher: InvocationMatcher,
        childType: KClass<*>,
    ): Any =
        InternalPlatform.synchronized(childs) {
            gatewayAccess.safeToString.exec {
                childs.customComputeIfAbsent(matcher) {
                    gatewayAccess.mockFactory!!.mockk(
                        childType,
                        childName(this.name),
                        moreInterfaces = arrayOf(),
                        relaxed = relaxed,
                        relaxUnitFun = relaxUnitFun,
                    )
                }
            }
        }

    internal fun getChildrenKeys() = childs.values

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
        fieldValueProvider: BackingFieldValueProvider,
    ): Any? {
        val originalPlusToString = {
            if (method.isToString()) {
                toStr()
            } else {
                originalCall()
            }
        }

        fun List<StackElement>.cutMockKCallProxyCall(): List<StackElement> {
            fun search(
                cls: String,
                mtd: String,
            ): Int? =
                indexOfFirst {
                    it.className == cls && it.methodName == mtd
                }.let { if (it == -1) null else it }

            val idx =
                search("io.mockk.proxy.MockKCallProxy", "call")
                    ?: search("io.mockk.proxy.MockKProxyInterceptor", "intercept")
                    ?: search("io.mockk.proxy.MockKProxyInterceptor", "interceptNoSuper")
                    ?: return this

            return this.drop(idx + 1)
        }

        val stackTraceHolder = InternalPlatform.captureStackTrace()

        val invocation =
            Invocation(
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
                fieldValueProvider,
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
            this.unansweredCalls.clear()
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

    private data class InvocationAnswer(
        val matcher: InvocationMatcher,
        val answer: Answer<*>,
        var usageCount: Int,
        var lenient: Boolean = false,
    )

    protected fun Invocation.allEqMatcher() =
        InvocationMatcher(
            self,
            method,
            args.map {
                if (it == null) {
                    NullCheckMatcher()
                } else {
                    EqMatcher(it)
                }
            },
            false,
        )

    private fun MethodDescription.typedSignature(): String =
        "$name(${paramTypes.joinToString(
            ", ",
        ) { it.simpleName ?: it.toString() }}): ${returnType.simpleName ?: returnType.toString()}${if (returnTypeNullable) "?" else ""}"

    override fun dispose() {
        clear(
            MockKGateway.ClearOptions(
                answers = true,
                recordedCalls = true,
                childMocks = true,
                verificationMarks = true,
                exclusionRules = true,
            ),
        )
        disposeRoutine.invoke()
    }
}
