package io.mockk.jvm

import io.mockk.InternalPlatform
import io.mockk.InternalPlatform.hkd
import io.mockk.InternalPlatform.toStr
import io.mockk.MethodDescription
import io.mockk.MockKException
import io.mockk.MockKGateway.MockFactory
import io.mockk.agent.MockKAgentException
import io.mockk.common.StubRepository
import io.mockk.impl.Logger
import io.mockk.impl.MockKStub
import io.mockk.impl.SpyKStub
import io.mockk.proxy.MockKProxyMaker
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.Callable
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

class JvmMockFactory(val proxyMaker: MockKProxyMaker,
                     val instantiator: JvmInstantiator,
                     val stubRepository: StubRepository) : MockFactory {
    override fun <T : Any> mockk(cls: KClass<T>, name: String?, moreInterfaces: Array<out KClass<*>>): T {
        val newName = name ?: "#${newId()}"
        log.debug { "Creating mockk for ${cls.toStr()} name=$newName, moreInterfaces=${Arrays.toString(moreInterfaces)}" }

        val stub = MockKStub(cls, newName)

        log.trace { "Building proxy for ${cls.toStr()} hashcode=${hkd(cls)}" }
        val proxy = try {
            proxyMaker.proxy(cls.java,
                    moreInterfaces.map { it.java }.toTypedArray(),
                    handler(stub),
                    false)
        } catch (ex: MockKAgentException) {
            throw MockKException("Can't instantiate proxy for " + cls, ex)
        }

        stub.hashCodeStr = hkd(proxy)

        stubRepository.add(proxy, stub)

        return cls.cast(proxy)
    }

    override fun <T : Any> spyk(cls: KClass<T>?, objToCopy: T?, name: String?, moreInterfaces: Array<out KClass<*>>): T {
        val newName = name ?: "#${newId()}"
        log.debug { "Creating spyk for ${cls.toStr()} name=$newName, moreInterfaces=${Arrays.toString(moreInterfaces)}" }

        val actualCls = when {
            objToCopy != null -> objToCopy::class
            cls != null -> cls
            else -> throw MockKException("Either cls or objToCopy should not be null")
        }

        val stub = SpyKStub(actualCls, newName)

        val useDefaultConstructor = objToCopy == null

        log.trace { "Building proxy for ${actualCls.toStr()} hashcode=${hkd(actualCls)}" }

        val proxy = try {
            proxyMaker.proxy(
                    actualCls.java,
                    moreInterfaces.map { it.java }.toTypedArray(),
                    handler(stub),
                    useDefaultConstructor)
        } catch (ex: MockKAgentException) {
            if (useDefaultConstructor) {
                throw MockKException("Can't instantiate proxy via default constructor for " + actualCls, ex)
            } else {
                throw MockKException("Can't instantiate proxy for " + actualCls, ex)
            }
        }

        stub.hashCodeStr = hkd(proxy)

        if (objToCopy != null) {
            copyFields(proxy, objToCopy, objToCopy.javaClass)
        }

        stubRepository.add(proxy, stub)

        return actualCls.cast(proxy)
    }

    private fun copyFields(obj: Any, objToCopy: Any, cls: Class<*>) {
        for (field in cls.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) {
                continue
            }
            field.isAccessible = true
            val value = field.get(objToCopy)
            field.set(obj, value)
            log.trace { "Copied field $field of $cls" }
        }
        if (cls.superclass != null) {
            copyFields(obj, objToCopy, cls.superclass)
        }
    }

    override fun staticMockk(cls: KClass<*>) {
        log.debug { "Creating static mockk for ${cls.toStr()}" }

        val stub = MockKStub(cls, "static " + cls.simpleName)

        log.trace { "Building static proxy for ${cls.toStr()} hashcode=${hkd(cls)}" }
        try {
            proxyMaker.staticProxy(cls.java, handler(stub))
        } catch (ex: MockKAgentException) {
            throw MockKException("Failed to build static proxy", ex)
        }

        stub.hashCodeStr = hkd(cls.java)

        stubRepository.add(cls.java, stub)
    }

    override fun staticUnMockk(cls: KClass<*>) {
        proxyMaker.staticUnProxy(cls.java)
    }

    override fun childMock(cls: KClass<*>): Any {
        val stub = MockKStub(cls, "temporary mock");

        log.trace { "Building proxy for ${cls.toStr()} hashcode=${hkd(cls)}" }

        val proxy = try {
            proxyMaker.proxy(
                    cls.java,
                    arrayOf(),
                    handler(stub),
                    false)
        } catch (ex: MockKAgentException) {
            log.trace(ex) {
                "Failed to build proxy for ${cls.toStr()}. " +
                        "Trying just instantiate it. " +
                        "This can help if it's last call in the chain"
            }

            instantiator.instantiate(cls)
        }

        stub.hashCodeStr = hkd(proxy)

        return proxy
    }


    private fun handler(stub: MockKStub): (Any, Method, Callable<*>, Array<Any?>) -> Any? {
        return { self, method, originalMethod, args ->
            stdFunctions(self, method, args) {
                stub.handleInvocation(self, method.toDescription(), {
                    handleOriginalCall(originalMethod, method)
                }, args)
            }
        }
    }

    protected inline fun stdFunctions(self: Any,
                                      method: Method,
                                      args: Array<Any?>,
                                      otherwise: () -> Any?): Any? {
        if (self is Class<*>) {
            if (method.isHashCode()) {
                return System.identityHashCode(self)
            } else if (method.isEquals()) {
                return self === args[0]
            }
        }
        return otherwise()
    }

    private fun handleOriginalCall(originalMethod: Callable<*>?, method: Method): Any? {
        if (originalMethod == null) {
            throw MockKException("No way to call original method ${method.toDescription()}")
        }

        return try {
            originalMethod.call()
        } catch (ex: InvocationTargetException) {
            throw MockKException("Failed to execute original call. Check cause please", ex.cause)
        }
    }

    override fun clear(mocks: Array<out Any>, answers: Boolean, recordedCalls: Boolean, childMocks: Boolean) {
        for (mock in mocks) {
            stubRepository.stubFor(mock).clear(answers, recordedCalls, childMocks)
        }
    }

    companion object {
        val idCounter = InternalPlatform.counter()

        fun newId(): Long = idCounter()

        val log = Logger<JvmMockFactory>()

        fun Method.toDescription() =
                MethodDescription(name, returnType.kotlin, declaringClass.kotlin, parameterTypes.map { it.kotlin })

        fun Method.isHashCode() = name == "hashCode" && parameterTypes.isEmpty()
        fun Method.isEquals() = name == "equals" && parameterTypes.size == 1 && parameterTypes[0] === Object::class.java
    }

}

