package io.mockk.proxy.jvm

import java.lang.reflect.Method

internal object HiddenClasses {
    /**
     * `Class.isHidden()` exists since JDK 15; resolved reflectively so that this file stays
     * loadable on JDK 8-14, where VM-anonymous lambda classes pose the same constraints.
     */
    private val isHiddenMethod: Method? =
        runCatching {
            Class::class.java.getMethod("isHidden")
        }.getOrNull()

    /**
     * `true` for classes that no other class can reference by name: hidden classes
     * (JDK 15+, e.g. lambdas compiled by Kotlin 2 with `invokedynamic`, named
     * `Outer$$Lambda$13/0x...`) and their VM-anonymous predecessors on older JDKs.
     *
     * Such classes cannot be subclassed (a subclass would need to resolve the hidden class
     * by name and fail with [NoClassDefFoundError]) and cannot be instrumented
     * (`Instrumentation.isModifiableClass` is `false`, see JEP 371), so neither objenesis
     * instantiation nor the inline agent can work on them.
     */
    fun isHidden(clazz: Class<*>): Boolean {
        val hiddenByApi =
            runCatching {
                isHiddenMethod?.invoke(clazz) == true
            }.getOrDefault(false)

        return hiddenByApi || clazz.name.contains("\$\$Lambda\$")
    }
}
