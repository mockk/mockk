package io.mockk.impl.interception

interface Interceptor {
    interface ArgProvider {
        fun arg(n: Int, value: Any?)
    }

    fun <T> intercept(
            args: ArgProvider.() -> Unit,
            block: () -> T
    ): T
}

