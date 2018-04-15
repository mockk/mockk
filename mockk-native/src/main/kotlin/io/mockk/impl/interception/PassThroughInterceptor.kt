package io.mockk.impl.interception

object PassThroughInterceptor : Interceptor {
    override fun <T> intercept(args: Interceptor.ArgProvider.() -> Unit, block: () -> T) = block()
}