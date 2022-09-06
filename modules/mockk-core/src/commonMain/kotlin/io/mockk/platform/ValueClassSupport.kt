package io.mockk.platform

import kotlin.reflect.KClass


expect object ValueClassSupport {

    /**
     * Underlying property value of a **`value class`** or self.
     *
     * The type of the return might also be a `value class`!
     */
    val <T : Any> T.boxedValue: Any?

    /**
     * Underlying property class of a **`value class`**, or self.
     *
     * The result might also be a value class! So check recursively, if necessary.
     */
    val KClass<*>.boxedClass: KClass<*>

}
