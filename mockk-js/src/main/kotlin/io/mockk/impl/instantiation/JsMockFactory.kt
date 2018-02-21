package io.mockk.impl.instantiation

import io.mockk.MethodDescription
import io.mockk.impl.log.Logger
import io.mockk.impl.stub.Stub
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.impl.stub.StubRepository
import kotlin.reflect.KClass

class JsMockFactory(
    stubRepository: StubRepository,
    instantiator: JsInstantiator,
    gatewayAccess: StubGatewayAccess
) :
    AbstractMockFactory(
        stubRepository,
        instantiator,
        gatewayAccess
    ) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> newProxy(
        cls: KClass<out T>,
        moreInterfaces: Array<out KClass<*>>,
        stub: Stub,
        useDefaultConstructor: Boolean,
        instantiate: Boolean
    ): T {
        return Proxy(
            StubProxyTarget(stub),
            StubProxyHandler(cls, stub)
        ) as T

    }

    companion object {
        val log = Logger<JsMockFactory>()
    }
}

internal external interface ProxyHandler {
    fun get(target: dynamic, name: String, receiver: dynamic): Any
    fun apply(target: dynamic, thisValue: dynamic, args: Array<*>): Any?
}

internal abstract class EmptyProxyHandler : ProxyHandler {
    protected fun isJsNativeMethods(name: String) =
        name in listOf("kotlinHashCodeValue\$", "\$metadata\$", "prototype", "constructor", "toString")

    override fun get(target: dynamic, name: String, receiver: dynamic): Any = throw UnsupportedOperationException("get")
    override fun apply(target: dynamic, thisValue: dynamic, args: Array<*>): Any? =
        throw UnsupportedOperationException("apply")
}

internal external class Proxy(target: dynamic, handler: ProxyHandler)


internal class OperationProxyHandler(
    val name: String,
    val stub: Stub,
    val cls: KClass<*>,
    val receiver: dynamic
) : EmptyProxyHandler() {

    override fun get(target: dynamic, name: String, receiver: dynamic): Any {
        if (isJsNativeMethods(name)) {
            return target[name]
        }
        return super.get(target, name, receiver)
    }

    fun originalCall(target: dynamic, thisValue: dynamic, args: Array<*>): Any? {
        return js("target.apply(thisValue, args)")
    }

    override fun apply(target: dynamic, thisValue: dynamic, args: Array<*>): Any? {
        return stub.handleInvocation(receiver,
            MethodDescription(
                name,
                Any::class,
                cls,
                listOf(),
                -1,
                false
            ),
            { originalCall(target, thisValue, args) },
            args.map { unboxChar(it) }.toTypedArray()
        )
    }

    private fun unboxChar(value: Any?): Any? {
        if (value is Char) {
            return value.toInt()
        } else {
            return value
        }
    }
}

internal class StubProxyTarget(val stub: Stub) {
    override fun toString() = stub.toStr()
}

internal class StubProxyHandler(val cls: KClass<*>, val stub: Stub) : EmptyProxyHandler() {
    override fun get(target: dynamic, name: String, receiver: dynamic): Any {
        if (isJsNativeMethods(name) || name == "stub") {
            return target[name]
        }

        val targetMember = if (checkKeyExists(name, target)) {
            if (checkJsFunction(target[name])) {
                target[name]
            } else {
                return target[name]
            }
        } else {
            js("function (){}")
        }
        return Proxy(
            targetMember,
            OperationProxyHandler(name, stub, cls, receiver)
        )
    }

    private fun checkKeyExists(name: String, target: dynamic): Boolean = js("name in target")
    private fun checkJsFunction(value: dynamic): Boolean = js("value instanceof Function")
}
