package io.mockk.impl

import io.mockk.*
import javassist.util.proxy.MethodHandler
import javassist.util.proxy.ProxyObject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*


internal interface MockKInstance : MockK {
    val ___id: Long

    fun ___type(): Class<*>

    fun ___addAnswer(matcher: InvocationMatcher, answer: Answer<*>)

    fun ___answer(invocation: Invocation): Any?

    fun ___childMockK(call: Call): MockKInstance

    fun ___recordCall(invocation: Invocation)

    fun ___matchesAnyRecordedCalls(matcher: InvocationMatcher, min: Int, max: Int): Boolean

    fun ___allRecordedCalls(): List<Invocation>

    fun ___clear(answers: Boolean, calls: Boolean, childMocks: Boolean)
}

private data class InvocationAnswer(val matcher: InvocationMatcher, val answer: Answer<*>)

internal open class MockKInstanceProxyHandler(private val cls: Class<*>,
                                              private val id: Long,
                                              private val obj: Any) : MethodHandler, MockKInstance {
    private val answers = Collections.synchronizedList(mutableListOf<InvocationAnswer>())
    private val childs = Collections.synchronizedMap(hashMapOf<InvocationMatcher, MockKInstance>())
    private val recordedCalls = Collections.synchronizedList(mutableListOf<Invocation>())

    override val ___id: Long = id

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

    override fun toString() = "mockk<${___type().simpleName}>()#$___id"

    override fun equals(other: Any?): Boolean {
        return obj === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(obj)
    }

    override fun ___childMockK(call: Call): MockKInstance {
        return synchronized(childs) {
            childs.java6ComputeIfAbsent(call.matcher) {
                MockKGateway.LOCATOR().mockFactory.mockk(call.retType) as MockKInstance
            }
        }
    }

    override fun invoke(self: Any,
                        thisMethod: Method,
                        proceed: Method?,
                        args: Array<out Any?>): Any? {

        findMethodInProxy(this, thisMethod)?.let {
            try {
                return it.invoke(this, *args)
            } catch (ex: InvocationTargetException) {
                throw ex.demangle()
            }
        }

        val argList = args.toList()
        val invocation = Invocation(self as MockKInstance, thisMethod, proceed, argList)
        return MockKGateway.LOCATOR().callRecorder.call(invocation)
    }

    private fun findMethodInProxy(obj: Any,
                                  method: Method): Method? {
        return obj.javaClass.methods.find {
            proxiableMethod(it) && sameSignature(it, method)
        }
    }

    private fun sameSignature(method1: Method, method2: Method) =
            method1.name == method2.name && Arrays.equals(method1.parameterTypes, method2.parameterTypes)

    private fun proxiableMethod(method: Method): Boolean {
        val name = method.name

        return name.startsWith("___")
                || name.equals("toString")
                || name.equals("equals")
                || name.equals("hashCode")
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


internal class SpyKInstanceProxyHandler<T>(cls: Class<T>, id: Long, obj: ProxyObject) : MockKInstanceProxyHandler(cls, id, obj) {
    override fun ___defaultAnswer(invocation: Invocation): Any? {
        if (invocation.superMethod == null) {
            throw MockKException("no super method for: ${invocation.method}")
        }
        return invocation.superMethod.invoke(invocation.self, *invocation.args.toTypedArray())
    }

    override fun toString(): String = "spyk<" + ___type().simpleName + ">()#$___id"
}

