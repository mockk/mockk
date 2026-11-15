package io.mockk.proxy.jvm.advice

import io.mockk.proxy.MockKInvocationHandler
import java.lang.reflect.Method

/**
 * Tracks a proxy that is currently being instantiated through its real default constructor.
 *
 * `spyk(SomeClass::class)` builds the proxy by running the class's own default constructor,
 * so field initializers and the Kotlin `init` block execute before the proxy exists and its
 * handler can be registered in the per-instance handler map. Calls made on the instance
 * during that window would be lost: the advice checks the handler map, finds nothing and
 * lets the original code run unintercepted (#1288).
 *
 * While the constructor runs, [during] holds the class and its handler on a thread local;
 * [handlerFor] and [matches] let the advice route calls made on the instance being built to
 * that handler. Matching is by class rather than by instance identity, because the instance
 * does not exist yet while its constructor runs. If the constructor synchronously builds
 * other instances of the same class and calls their methods, those calls reach the handler
 * too; they still execute their original implementations, so behavior is preserved.
 *
 * Calls the JDK makes to itself while the window is open are never routed: reflection
 * accessibility checks, collection hashing and similar machinery run inside the window
 * (the constructor itself calls `Constructor.setAccessible`) and their receivers are
 * arbitrary instances, not the proxy under construction. [BaseAdvice] screens the fallback
 * with [JdkInternalCallGuard] for the same reason the static advice does (#1402).
 *
 * No window is opened for [java.lang.Object] (`spyk<Any>()`): Object declares no methods a
 * constructor could usefully record, and a class-matched window on it would match every
 * object in the JVM.
 */
internal object ConstructingInstance {
    private val current = ThreadLocal<State?>()

    private class State(
        val cls: Class<*>,
        val handler: MockKInvocationHandler,
    )

    fun <T : Any> during(
        cls: Class<*>,
        handler: MockKInvocationHandler,
        block: () -> T,
    ): T {
        if (cls === Object::class.java) {
            return block()
        }

        val previous = current.get()
        current.set(State(cls, handler))
        return try {
            block()
        } finally {
            current.set(previous)
        }
    }

    fun matches(self: Any?): Boolean {
        val state = current.get() ?: return false
        return self != null && state.cls.isInstance(self)
    }

    fun handlerFor(
        self: Any?,
        method: Method?,
    ): MockKInvocationHandler? {
        if (!matches(self)) {
            return null
        }
        if (method == null || method.declaringClass === Object::class.java) {
            return null
        }
        return current.get()!!.handler
    }
}
