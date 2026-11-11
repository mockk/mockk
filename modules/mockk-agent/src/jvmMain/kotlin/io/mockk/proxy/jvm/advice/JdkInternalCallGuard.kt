package io.mockk.proxy.jvm.advice

import java.lang.StackWalker
import java.lang.reflect.Method

/**
 * Detects static calls that the JDK makes to itself, so a static mock can let them reach the
 * original implementation instead of the mock handler.
 *
 * A static mock instruments every static method of the target class, so the JVM's own internal
 * calls are intercepted as well. On JDK 9+ the JDK reads classpath jars through
 * [java.nio.file.Files]: `ZipFile` stats the file with `Files.readAttributes`, and the
 * `ServiceLoader` scans that kotlin-reflect performs while initializing its type checker
 * (`OverridingUtil` static init) open classpath jars through the same path. With `Files`
 * statically mocked those calls reach the mock handler while the JVM is in the middle of reading
 * the very classpath the handler's machinery needs. The handler consults kotlin-reflect (method
 * descriptions, value class detection), which lazily initializes its type checker and builds
 * package scopes from the classpath that is still being read — the re-entrant interception fails
 * those lazy computations with an `ExceptionInInitializerError` or
 * "Recursion detected in a lazy value", leaving kotlin-reflect unusable for the rest of the JVM
 * (issue #1402).
 *
 * Calls from JDK code (bootstrap or platform class loader) therefore bypass the mock and run the
 * original implementation. Code on the application classpath — user tests, libraries,
 * kotlin-stdlib — is still intercepted as usual. The check only runs while a static mock for the
 * class is active; once the mock is gone the transformed class keeps a cheap pass-through.
 */
object JdkInternalCallGuard {
    private val walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
    private val platformClassLoader: ClassLoader? =
        try {
            ClassLoader.getPlatformClassLoader()
        } catch (_: Throwable) {
            // Pre-JDK 9: only the bootstrap loader exists there.
            null
        }

    /**
     * Returns `true` when the first frame above the intercepted method belongs to JDK code.
     *
     * The advice code that calls this is inlined into the intercepted method itself, so the
     * frames declaring [interceptedClass] with [method]'s name are the intercepted method (and
     * its bridge, if one was generated); the first frame beyond them is whoever called it.
     */
    fun isJdkInternalCall(
        interceptedClass: Class<*>,
        method: Method,
    ): Boolean =
        try {
            walker.walk { frames ->
                var seenInterceptedMethod = false
                var callerClassLoader: ClassLoader? = null
                for (frame in frames) {
                    val isInterceptedMethod =
                        frame.declaringClass == interceptedClass &&
                            frame.methodName == method.name

                    when {
                        !seenInterceptedMethod && isInterceptedMethod -> seenInterceptedMethod = true
                        !seenInterceptedMethod -> continue
                        isInterceptedMethod -> continue
                        else -> {
                            callerClassLoader = frame.declaringClass.classLoader
                            break
                        }
                    }
                }
                seenInterceptedMethod &&
                    (
                        callerClassLoader == null ||
                            callerClassLoader === platformClassLoader
                    )
            }
        } catch (_: Throwable) {
            // Detection is best-effort: if the stack cannot be inspected, intercept as usual.
            false
        }
}
