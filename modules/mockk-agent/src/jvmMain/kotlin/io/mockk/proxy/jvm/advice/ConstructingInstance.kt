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
 * While the constructor runs, [during] holds the class and its handler; [handlerFor] and
 * [matches] let the advice route calls made on the instance being built to that handler.
 * Matching is by class rather than by instance identity, because the instance does not exist
 * yet while its constructor runs. If the constructor synchronously builds other instances of
 * the same class and calls their methods, those calls reach the handler too; they still
 * execute their original implementations, so behavior is preserved.
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
 *
 * The window state itself lives in [BaseAdvice]'s companion: the advice consults it on every
 * intercepted call, including the ones coverage-agent probes make through the mockk-advised
 * `Object.equals`, and a class referenced there must already be initialized when that happens
 * — see [BaseAdvice] for details.
 */
internal object ConstructingInstance {
    fun <T : Any> during(
        cls: Class<*>,
        handler: MockKInvocationHandler,
        block: () -> T,
    ): T = BaseAdvice.duringConstructing(cls, handler, block)

    fun matches(self: Any?): Boolean = BaseAdvice.constructingMatches(self)

    fun handlerFor(
        self: Any?,
        method: Method?,
    ): MockKInvocationHandler? = BaseAdvice.constructingHandlerFor(self, method)
}
