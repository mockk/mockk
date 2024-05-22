package io.mockk.core

import java.lang.reflect.Method
import kotlin.reflect.KClass


expect object ValueClassSupport {

    /**
     * Unboxes the underlying property value of a **`value class`** or self, as long the unboxed value is appropriate
     * for the given method's return type.
     *
     * @see boxedValue
     */
    fun <T : Any> T.maybeUnboxValueForMethodReturn(method: Method): Any?

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
