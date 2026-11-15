package io.mockk.proxy.jvm.advice

import io.mockk.proxy.MockKInvocationHandler
import io.mockk.proxy.jvm.advice.jvm.MockHandlerMap
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher
import java.lang.reflect.Method
import java.util.Random
import java.util.concurrent.Callable

internal open class BaseAdvice(
    private val handlers: MockHandlerMap,
) : JvmMockKDispatcher() {
    val id = randomGen.nextLong()

    override fun handler(
        self: Any,
        method: Method,
        arguments: Array<Any?>,
    ): Callable<*>? {
        val handler =
            handlers[self]
                ?: constructingHandlerFor(self, method)?.takeIf {
                    // Calls the JDK makes to itself while a construction window is open run
                    // the original implementation, like for active static mocks (#1402): the
                    // receivers of those calls are arbitrary instances, not the proxy under
                    // construction (#1288).
                    !JdkInternalCallGuard.isJdkInternalCall(self.javaClass, method)
                }
                ?: return null

        if (SelfCallEliminator.isSelf(self, method)) {
            return null
        }

        if (!method.isBridge) {
            return Interceptor(handler, self, method, arguments)
        }

        val bridgedMethod =
            BridgeMethodResolver.findBridgedMethod(method, arguments)
                ?: return null

        return Interceptor(handler, self, bridgedMethod, arguments, originalMethod = method)
    }

    override fun constructorDone(
        self: Any,
        arguments: Array<Any?>,
    ) {
        val handler =
            handlers[self::class.java]
                ?: return

        handler.invocation(self, null, null, arguments)
    }

    override fun handle(
        self: Any,
        method: Method,
        arguments: Array<Any?>,
        originalMethod: Callable<Any>?,
    ): Any? {
        val handler =
            handler(self, method, arguments)
                ?: originalMethod

        return handler?.call()
    }

    override fun isMock(instance: Any): Boolean {
        // in order to avoid endless checks when concurrent hashmap is mocked we need to exclude handlers map explicitly
        return handlers.isMock(instance) || constructingMatches(instance)
    }

    companion object {
        private val randomGen = Random()

        /**
         * State of a proxy that is currently being instantiated through its real default
         * constructor, held on a thread local owned by [BaseAdvice] itself (#1288).
         *
         * It cannot live in a separate object: [isMock] is reachable from the mockk-advised
         * `java.lang.Object.equals`, because mocking any class instruments its whole superclass
         * chain up to `Object`. Coverage agents probe every class initialization, and their probe
         * compares arrays through `Object.equals`, so the first call hitting a not-yet-initialized
         * class referenced from [isMock] re-enters that class's own half-done initialization and
         * poisons it (`ExceptionInInitializerError`, then `NoClassDefFoundError` for the rest of
         * the JVM). `BaseAdvice` is initialized when the agent creates its advice instances,
         * before any code instrumented by those instances can run, so its state is always
         * already initialized on this path.
         */
        private class ConstructingState(
            val cls: Class<*>,
            val handler: MockKInvocationHandler,
        )

        private val constructing = ThreadLocal<ConstructingState?>()

        fun <T : Any> duringConstructing(
            cls: Class<*>,
            handler: MockKInvocationHandler,
            block: () -> T,
        ): T {
            if (cls === Object::class.java) {
                return block()
            }

            val previous = constructing.get()
            constructing.set(ConstructingState(cls, handler))
            return try {
                block()
            } finally {
                constructing.set(previous)
            }
        }

        fun constructingMatches(self: Any?): Boolean {
            val state = constructing.get() ?: return false
            return self != null && state.cls.isInstance(self)
        }

        fun constructingHandlerFor(
            self: Any?,
            method: Method?,
        ): MockKInvocationHandler? {
            if (!constructingMatches(self)) {
                return null
            }
            if (method == null || method.declaringClass === Object::class.java) {
                return null
            }
            return constructing.get()!!.handler
        }
    }
}
