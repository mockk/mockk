package io.mockk.proxy.jvm.transformation

import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.jvm.ClassLoadingStrategyChooser
import io.mockk.proxy.jvm.advice.ProxyAdviceId
import io.mockk.proxy.jvm.advice.jvm.JvmMockKProxyInterceptor
import io.mockk.proxy.jvm.advice.jvm.MockHandlerMap
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher
import net.bytebuddy.ByteBuddy
import net.bytebuddy.TypeCache
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.loading.MultipleParentClassLoader
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.attribute.MethodAttributeAppender
import net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
import net.bytebuddy.matcher.ElementMatchers.any
import net.bytebuddy.matcher.ElementMatchers.isAbstract
import net.bytebuddy.matcher.ElementMatchers.isDefaultFinalizer
import net.bytebuddy.matcher.ElementMatchers.isSynthetic
import net.bytebuddy.matcher.ElementMatchers.not
import java.io.File
import java.lang.Thread.currentThread
import java.util.concurrent.atomic.AtomicLong

internal class SubclassInstrumentation(
    private val log: MockKAgentLogger,
    private val handlers: MockHandlerMap,
    private val byteBuddy: ByteBuddy,
) {
    private val bootstrapMonitor = Any()
    private val proxyClassCache = TypeCache<CacheKey>(TypeCache.Sort.WEAK)
    private lateinit var interceptor: JvmMockKProxyInterceptor

    /**
     * Byte Buddy ignores synthetic methods by default, so a subclass proxy would leave any abstract
     * method flagged `ACC_SYNTHETIC` unimplemented and fail with an [AbstractMethodError] when it is
     * called. Kotlin flags members annotated with `@JvmSynthetic` (including the `@get:`/`@set:`
     * targeted forms on a property) that way, so such members have to be intercepted like any other.
     *
     * Synthetic *concrete* methods keep being ignored. Bridge methods are the important ones here:
     * they are always generated with a body - as `default` methods on interfaces - even when the
     * method they delegate to is abstract, so ignoring them leaves them to forward to the real
     * signature, which is intercepted. Intercepting the bridges instead would record calls against
     * the erased signature.
     */
    private val subclassingByteBuddy =
        byteBuddy.ignore(
            isSynthetic<MethodDescription>()
                .and(not(isAbstract()))
                .or(isDefaultFinalizer()),
        )

    init {
        class AdviceBuilder {
            fun build() {
                interceptor = JvmMockKProxyInterceptor(handlers)

                JvmMockKDispatcher.set(interceptor.id, interceptor)
            }
        }
        AdviceBuilder().build()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> subclass(
        clazz: Class<T>,
        interfaces: Array<Class<*>>,
    ): Class<T> {
        val key = CacheKey(clazz, interfaces.toSet())
        val classLoader = clazz.classLoader
        val monitor = classLoader ?: bootstrapMonitor

        return proxyClassCache.findOrInsert(
            classLoader,
            key,
            { doInterceptedSubclassing(clazz, interfaces) },
            monitor,
        ) as Class<T>
    }

    private fun <T> doInterceptedSubclassing(
        clazz: Class<T>,
        interfaces: Array<Class<*>>,
    ): Class<out T> {
        val resultClassLoader =
            MultipleParentClassLoader
                .Builder()
                .append(clazz)
                .append(*interfaces)
                .append(currentThread().contextClassLoader)
                .append(JvmMockKProxyInterceptor::class.java)
                .build(JvmMockKProxyInterceptor::class.java.classLoader)

        val interceptor =
            MethodDelegation
                .withDefaultConfiguration()
                .withBinders(
                    TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant.of(
                        ProxyAdviceId::class.java,
                        interceptor.id,
                    ),
                ).to(JvmMockKProxyInterceptor::class.java)

        val type =
            subclassingByteBuddy
                .subclass(clazz)
                .implement(*interfaces)
                .annotateType(*clazz.annotations)
                .method(any<Any>())
                .intercept(interceptor)
                .attribute(MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER)
                .make()

        try {
            val property = System.getProperty("io.mockk.classdump.path")
            if (property != null) {
                val nextIndex = classDumpIndex.incrementAndGet().toString()
                val storePath = File(File(property, "subclass"), nextIndex)
                type.saveIn(storePath)
            }
        } catch (ex: Exception) {
            log.trace(ex, "Failed to save file to a dump")
        }

        return type
            .load(resultClassLoader, chooseClassLoadingStrategy(clazz))
            .loaded
    }

    private fun <T> chooseClassLoadingStrategy(clazz: Class<T>) = ClassLoadingStrategyChooser.chooseClassLoadingStrategy(clazz)

    companion object {
        val classDumpIndex = AtomicLong()
    }
}
