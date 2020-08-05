package io.mockk.proxy.jvm.advice

import io.mockk.proxy.jvm.advice.jvm.MockHandlerMap
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher
import java.lang.reflect.Method
import java.util.Random
import java.util.concurrent.Callable

internal open class BaseAdvice(
    private val handlers: MockHandlerMap
) : JvmMockKDispatcher() {
    val id = randomGen.nextLong()

    override fun handler(self: Any, method: Method, arguments: Array<Any?>): Callable<*>? {
        val handler = handlers[self]
                ?: return null

        return if (SelfCallEliminator.isSelf(self, method)) {
            null
        } else {
            Interceptor(handler, self, method, arguments)
        }

    }

    override fun constructorDone(
        self: Any,
        arguments: Array<Any?>
    ) {
        val handler = handlers[self::class.java]
                ?: return

        handler.invocation(self, null, null, arguments)
    }


    override fun handle(self: Any, method: Method, arguments: Array<Any?>, originalMethod: Callable<Any>?): Any? {
        val handler =
            handler(self, method, arguments)
                    ?: originalMethod

        return handler?.call()
    }

    override fun isMock(instance: Any): Boolean {
        // in order to avoid endless checks when concurrent hashmap is mocked we need to exclude handlers map explicitly
        return handlers.isMock(instance)
    }

    companion object {
        private val randomGen = Random()
    }
}
