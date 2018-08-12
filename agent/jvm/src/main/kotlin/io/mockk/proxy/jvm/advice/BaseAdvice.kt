package io.mockk.proxy.jvm.advice

import io.mockk.proxy.ProxyInterceptionScope
import io.mockk.proxy.MockKInvocationHandler
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher
import io.mockk.proxy.safeScope
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.Callable

internal open class BaseAdvice(
    private val handlers: Map<Any, MockKInvocationHandler>,
    private val interceptionScope: ProxyInterceptionScope
) : JvmMockKDispatcher() {
    val id = randomGen.nextLong()

    override fun handler(self: Any, method: Method, arguments: Array<Any?>): Callable<*>? {
        val safeScope = interceptionScope.isInSafeScope()

        val systemCall = isSystemCall(method)

        return interceptionScope.safeScope(true) {

            val handler = handlers[self]
                    ?: return@safeScope null


            if (SelfCallEliminator.isSelf(self, method) || safeScope || systemCall) {
                return@safeScope null
            }

            return@safeScope buildInterceptor(self, method, arguments, handler)
        }
    }

    private fun isSystemCall(method: Method): Boolean {
        val declaringClassName = method.declaringClass.name
        if (!isSystemClasses(declaringClassName)) {
            return false
        }

        val stackTrace = Exception().stackTrace ?: return false
        println(stackTrace.size)
        for (i in 0 until stackTrace.size) {
            if (stackTrace[i].methodName == method.name &&
                stackTrace[i].className == declaringClassName &&
                i + 1 < stackTrace.size
            ) {
                val previousCall = stackTrace[i + 1]
                if (isSystemClasses(previousCall.className)) {
                    return true
                }

                break
            }
        }
        return false
    }

    private fun isSystemClasses(declaringClassName: String) = declaringClassName.startsWith("java.") ||
            declaringClassName.startsWith("sun.")

    private fun buildInterceptor(
        self: Any,
        method: Method,
        arguments: Array<Any?>,
        handler: MockKInvocationHandler
    ): Callable<Any?> {
        val originalMethodCall =
            interceptionScope.safeScope(
                false,
                SelfCallEliminatorCallable(
                    MethodCall(self, method, arguments),
                    self,
                    method
                )
            )

        return interceptionScope.safeScope(
            true,
            Interceptor(
                handler, self, method, arguments, originalMethodCall
            )
        )
    }

    override fun constructorDone(
        self: Any,
        arguments: Array<Any?>
    ) {
        val handler = handlers[self::class.java]
                ?: return

        if (interceptionScope.isInSafeScope()) {
            return
        }

        interceptionScope.safeScope(true) {
            handler.invocation(self, null, null, arguments)
        }
    }


    override fun handle(self: Any, method: Method, arguments: Array<Any?>, originalMethod: Callable<Any>?): Any? {
        val handler = handler(self, method, arguments)
                ?: originalMethod


        return handler?.call()
    }

    companion object {
        private val randomGen = Random()
    }
}

