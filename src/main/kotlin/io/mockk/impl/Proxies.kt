package io.mockk.impl

import io.mockk.*
import javassist.util.proxy.MethodHandler
import javassist.util.proxy.ProxyObject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*


internal interface MockKInstance : MockK {
    fun ___type(): Class<*>

    fun ___addAnswer(matcher: InvocationMatcher, answer: Answer<*>)

    fun ___answer(invocation: Invocation): Any?

    fun ___childMockK(call: Call): MockKInstance

    fun ___recordCall(invocation: Invocation)

    fun ___matchesAnyRecordedCalls(matcher: InvocationMatcher, min: Int, max: Int): Boolean

    fun ___allRecordedCalls(): List<Invocation>

    fun ___clear(answers: Boolean, calls: Boolean, childMocks: Boolean)
}

internal open class MockKInstanceProxyHandler(private val cls: Class<*>,
                                             private val obj: Any) : MethodHandler, MockKInstance {
    private val answers = Collections.synchronizedList(mutableListOf<InvocationAnswer>())
    private val childs = Collections.synchronizedMap(hashMapOf<InvocationMatcher, MockKInstance>())
    private val recordedCalls = Collections.synchronizedList(mutableListOf<Invocation>())

    override fun ___addAnswer(matcher: InvocationMatcher, answer: Answer<*>) {
        answers.add(InvocationAnswer(matcher, answer))
    }

    override fun ___answer(invocation: Invocation): Any? {
        val invocationAndMatcher = synchronized(answers) {
            answers
                    .reversed()
                    .firstOrNull { it.matcher.match(invocation) }
                    ?: return ___defaultAnswer(invocation)
        }

        return with(invocationAndMatcher) {
            ___captureAnswer(matcher, invocation)

            val call = Call(invocation.method.returnType,
                    invocation,
                    matcher, false)

            answer.answer(call)
        }
    }

    private fun ___captureAnswer(invocationMatcher: InvocationMatcher, invocation: Invocation) {
        repeat(invocationMatcher.args.size) {
            val argMatcher = invocationMatcher.args[it]
            if (argMatcher is CapturingMatcher) {
                argMatcher.capture(invocation.args[it])
            }
        }
    }

    protected open fun ___defaultAnswer(invocation: Invocation): Any? {
        throw MockKException("no answer found for: $invocation")
    }

    override fun ___recordCall(invocation: Invocation) {
        recordedCalls.add(invocation)
    }

    override fun ___matchesAnyRecordedCalls(matcher: InvocationMatcher, min: Int, max: Int): Boolean {
        synchronized(recordedCalls) {
            val n = recordedCalls.filter { matcher.match(it) }.count()
            return n in min..max
        }
    }

    override fun ___allRecordedCalls(): List<Invocation> {
        synchronized(recordedCalls) {
            return recordedCalls.toList()
        }
    }

    override fun ___type(): Class<*> = cls

    override fun toString() = "mockk<" + ___type().simpleName + ">()"

    override fun equals(other: Any?): Boolean {
        return obj === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(obj)
    }

    override fun ___childMockK(call: Call): MockKInstance {
        return childs.computeIfAbsent(call.matcher, {
            MockKGateway.LOCATOR().mockk(call.retType) as MockKInstance
        })
    }

    override fun invoke(self: Any,
                        thisMethod: Method,
                        proceed: Method?,
                        args: Array<out Any?>): Any? {

        findMethodInProxy(this, thisMethod)?.let {
            try {
                return it.invoke(this, *args)
            } catch (ex: InvocationTargetException) {
                var thr : Throwable = ex
                while (thr.cause != null &&
                        thr is InvocationTargetException) {
                    thr = thr.cause!!
                }
                throw thr
            }
        }

        val argList = args.toList()
        val invocation = Invocation(self as MockKInstance, thisMethod, proceed, argList)
        return MockKGateway.LOCATOR().callRecorder.call(invocation)
    }

    private fun findMethodInProxy(obj: Any,
                                  method: Method): Method? {
        return obj.javaClass.methods.find {
            it.name == method.name &&
                    Arrays.equals(it.parameterTypes, method.parameterTypes)
        }
    }

    override fun ___clear(answers: Boolean, calls: Boolean, childMocks: Boolean) {
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
}


internal class SpyKInstanceProxyHandler<T>(cls: Class<T>, obj: ProxyObject) : MockKInstanceProxyHandler(cls, obj) {
    override fun ___defaultAnswer(invocation: Invocation): Any? {
        if (invocation.superMethod == null) {
            throw MockKException("no super method for: ${invocation.method}")
        }
        return invocation.superMethod.invoke(invocation.self, *invocation.args.toTypedArray())
    }

    override fun toString(): String = "spyk<" + ___type().simpleName + ">()"
}

