package io.mockk.proxy.jvm.advice.jvm

import io.mockk.proxy.MockKInvocationHandler
import io.mockk.proxy.jvm.dispatcher.JvmMockKWeakMap
import java.util.Collections

class WeakMockHandlersMap(private val innerMap: JvmMockKWeakMap<Any, MockKInvocationHandler>) :
        MockHandlerMap,
        MutableMap<Any, MockKInvocationHandler> by innerMap {

    constructor() : this(JvmMockKWeakMap())

    override fun isMock(instance: Any): Boolean {
        return instance !== innerMap.target && innerMap.containsKey(instance)
    }
}

class SynchronizedMockHandlersMap(private val innerMap: MutableMap<Any, MockKInvocationHandler>) :
        MockHandlerMap,
        MutableMap<Any, MockKInvocationHandler> by innerMap {

    constructor() : this(Collections.synchronizedMap(mutableMapOf<Any, MockKInvocationHandler>()))

    override fun isMock(instance: Any): Boolean {
        return instance !== innerMap && innerMap.containsKey(instance)
    }
}

interface MockHandlerMap : MutableMap<Any, MockKInvocationHandler> {
    fun isMock(instance: Any): Boolean

    companion object {
        fun create(hasInstrumentation: Boolean): MockHandlerMap =
                if (hasInstrumentation)
                    WeakMockHandlersMap()
                else
                    SynchronizedMockHandlersMap()
    }
}
