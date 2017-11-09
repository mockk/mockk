package io.mockk.impl

import io.mockk.*
import javassist.util.proxy.MethodHandler
import javassist.util.proxy.ProxyObject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.KClass


internal interface MockKInstance : MockK {
    val ___name: String

    fun ___type(): KClass<*>

    fun ___addAnswer(matcher: InvocationMatcher, answer: Answer<*>)

    fun ___answer(invocation: Invocation): Any?

    fun ___childMockK(call: Call): MockKInstance

    fun ___recordCall(invocation: Invocation)

    fun ___allRecordedCalls(): List<Invocation>

    fun ___clear(answers: Boolean, calls: Boolean, childMocks: Boolean)
}

private data class InvocationAnswer(val matcher: InvocationMatcher, val answer: Answer<*>)

internal open class MockKInstanceProxyHandler(private val cls: KClass<*>,
                                              private val name: String,
                                              private val obj: Any) : MethodHandler, MockKInstance {
    private val answers = Collections.synchronizedList(mutableListOf<InvocationAnswer>())
    private val childs = Collections.synchronizedMap(hashMapOf<InvocationMatcher, MockKInstance>())
    private val recordedCalls = Collections.synchronizedList(mutableListOf<Invocation>())

    override val ___name: String = name

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

    override fun ___allRecordedCalls(): List<Invocation> {
        synchronized(recordedCalls) {
            return recordedCalls.toList()
        }
    }

    override fun ___type(): KClass<*> = cls

    override fun toString() = "mockk<${___type().simpleName}>($___name)"

    override fun equals(other: Any?): Boolean {
        return obj === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(obj)
    }

    override fun ___childMockK(call: Call): MockKInstance {
        return synchronized(childs) {
            childs.java6ComputeIfAbsent(call.matcher) {
                MockKGateway.implementation().mockFactory.mockk(
                        call.retType,
                        childOfName(___name),
                        moreInterfaces = arrayOf()) as MockKInstance
            }
        }
    }

    private fun childOfName(name: String): String {
        val result = childOfRegex.matchEntire(name)
        return if (result != null) {
            val group = result.groupValues[2]
            val childN = if (group.isEmpty()) 1 else Integer.parseInt(group)
            "child^" + (childN + 1) + " of " + result.groupValues[3]
        } else {
            "child of " + name
        }
    }

    override fun invoke(self: Any,
                        thisMethod: Method,
                        proceed: Method?,
                        args: Array<out Any?>): Any? {

        if (isFinalizeMethod(thisMethod)) {
            return null
        }

        findMethodInProxy(this, thisMethod)?.let {
            try {
                return it.invoke(this, *args)
            } catch (ex: InvocationTargetException) {
                throw ex.demangle()
            }
        }

        val argList = args.toList()
        val invocation = Invocation(self as MockKInstance,
                thisMethod.toDescription(),
                proceed?.toDescription(),
                argList,
                System.nanoTime())
        return MockKGateway.implementation().callRecorder.call(invocation)
    }

    private fun isFinalizeMethod(thisMethod: Method) = (thisMethod.name == "finalize"
            && thisMethod.parameterTypes.size == 0)

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

    companion object {
        val childOfRegex = Regex("child(\\^(\\d+))? of (.+)")
    }
}


internal class SpyKInstanceProxyHandler<T : Any>(cls: KClass<T>, name: String, obj: ProxyObject) : MockKInstanceProxyHandler(cls, name, obj) {
    override fun ___defaultAnswer(invocation: Invocation): Any? {
        val superMethod = invocation.superMethod
        if (superMethod == null) {
            throw MockKException("no super method for: ${invocation.method}")
        }
        return superMethod.invoke(invocation.self, *invocation.args.toTypedArray())
    }

    override fun toString(): String = "spyk<" + ___type().simpleName + ">($___name)"
}

private fun MethodDescription.invoke(self: Any, vararg args : Any?) =
        (langDependentRef as Method).invoke(self, *args)

private fun Method.toDescription() =
        MethodDescription(name, returnType.kotlin, declaringClass.kotlin, parameterTypes.map { it.kotlin }, this)
