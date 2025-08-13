package io.mockk.proxy.advice

import io.mockk.proxy.jvm.advice.SelfCallEliminator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

class SelfCallEliminatorTest {

    @Test
    fun `isSelf returns true when ChildClass overrides BaseClass method with type parameter`() {
        val childMethod = getMethod("method", ChildClass::class.java, String::class.java) // ChildClass: override fun method(t: String)
        val baseMethod = getMethod("method", BaseClass::class.java, Any::class.java) // BaseClass: open fun method(t: T)

        SelfCallEliminator.apply(ChildClass::class.java, childMethod) {
            assertTrue(SelfCallEliminator.isSelf(ChildClass::class.java, baseMethod))
        }

        SelfCallEliminator.apply(BaseClass::class.java, baseMethod) {
            assertTrue(SelfCallEliminator.isSelf(BaseClass::class.java, childMethod))
        }
    }

    @Test
    fun `isSelf returns false when methods are not self`() {
        val childMethod = getMethod("method", ChildClass::class.java, Int::class.java) // ChildClass: fun method(t: Int)
        val baseMethod = getMethod("method", BaseClass::class.java, Any::class.java)  // BaseClass: open fun method(t: T)

        SelfCallEliminator.apply(ChildClass::class.java, childMethod) {
            assertFalse(SelfCallEliminator.isSelf(ChildClass::class.java, baseMethod))
        }

        val otherMethod = getMethod("someOtherMethod", ChildClass::class.java, String::class.java) // ChildClass: fun someOtherMethod(t: String)

        SelfCallEliminator.apply(ChildClass::class.java, otherMethod) {
            assertFalse(SelfCallEliminator.isSelf(ChildClass::class.java, baseMethod))
        }
    }

    private fun getMethod(name: String, clazz: Class<*>, parameterType: Class<*>): Method {
        return clazz.getDeclaredMethod(name, parameterType)
    }

    abstract class BaseClass<T> {
        open fun method(t: T) {
            println("BaseClass method with type parameter: $t")
        }
    }

    class ChildClass : BaseClass<String>() {
        override fun method(t: String) {
            super.method(t)
            println("ChildClass method with parameter String: $t")
        }

        fun method(t: Int) {
            println("ChildClass method with parameter Int: $t")
        }

        fun someOtherMethod(t: String) {
            println("ChildClass someOtherMethod with parameter String: $t")
        }
    }
}
