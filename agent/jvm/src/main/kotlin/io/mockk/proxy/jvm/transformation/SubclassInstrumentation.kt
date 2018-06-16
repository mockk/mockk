package io.mockk.proxy.jvm.transformation

import io.mockk.proxy.MockKInvocationHandler
import io.mockk.proxy.jvm.advice.ProxyAdviceId
import io.mockk.proxy.jvm.advice.jvm.JvmMockKProxyInterceptor
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher
import net.bytebuddy.ByteBuddy
import net.bytebuddy.TypeCache
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.dynamic.loading.MultipleParentClassLoader
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
import net.bytebuddy.matcher.ElementMatchers.any
import java.lang.Thread.currentThread

internal class SubclassInstrumentation(
    private val handlers: Map<Any, MockKInvocationHandler>,
    private val byteBuddy: ByteBuddy
) {
    private val bootstrapMonitor = Any()
    private val proxyClassCache = TypeCache<CacheKey>(TypeCache.Sort.WEAK)
    private lateinit var interceptor: JvmMockKProxyInterceptor

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
        interfaces: Array<Class<*>>
    ): Class<T> {
        val key = CacheKey(clazz, interfaces.toSet())
        val classLoader = clazz.classLoader
        val monitor = classLoader ?: bootstrapMonitor

        return proxyClassCache.findOrInsert(
            classLoader,
            key,
            { doInterceptedSubclassing(clazz, interfaces) },
            monitor
        ) as Class<T>
    }

    private fun <T> doInterceptedSubclassing(
        clazz: Class<T>,
        interfaces: Array<Class<*>>
    ): Class<out T> {
        val resultClassLoader = MultipleParentClassLoader.Builder()
            .append(clazz)
            .append(*interfaces)
            .append(currentThread().contextClassLoader)
            .append(JvmMockKProxyInterceptor::class.java)
            .build(JvmMockKProxyInterceptor::class.java.classLoader)


        val interceptor = MethodDelegation.withDefaultConfiguration()
            .withBinders(
                TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant.of(
                    ProxyAdviceId::class.java, interceptor.id
                )
            )
            .to(JvmMockKProxyInterceptor::class.java)

        return byteBuddy.subclass(clazz)
            .implement(*interfaces)
            .method(any<Any>())
            .intercept(interceptor)
            .make()
            .load(resultClassLoader, ClassLoadingStrategy.Default.INJECTION)
            .loaded
    }
}