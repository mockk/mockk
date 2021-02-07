@file:JvmName("JVMMockKKt")
package io.mockk

import io.mockk.impl.JvmMockKGateway
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

actual object MockK {
    actual inline fun <T> useImpl(block: () -> T): T {
        MockKGateway.implementation = JvmMockKGateway.defaultImplementationBuilder
        return block()
    }
}

/**
 * Returns the defining top-level extension function's [Class].
 */
inline val KFunction<*>.declaringKotlinFile
    get() = checkNotNull(javaMethod) { "$this is not a top-level extension function" }
            .declaringClass.kotlin

/**
 * Builds a static mock. Any mocks of this function's declaring class are cancelled before it's mocked
 */
fun mockkStatic(vararg functions: KFunction<*>) =
        mockkStatic(*functions.map { it.declaringKotlinFile }.toTypedArray())

/**
 * Cancel static mocks.
 */
fun unmockkStatic(vararg functions: KFunction<*>) =
        unmockkStatic(*functions.map { it.declaringKotlinFile }.toTypedArray())

/**
 * Builds a static mock and unmocks it after the block has been executed.
 */
inline fun mockkStatic(vararg functions: KFunction<*>, block: () -> Unit) =
        mockkStatic(*functions.map { it.declaringKotlinFile }.toTypedArray(), block = block)
