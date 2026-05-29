package io.mockk.proxy.android.advice

import io.mockk.proxy.android.AndroidMockKMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.Method
import java.net.URLClassLoader

class AdviceOverrideDetectionTest {
    @Test
    fun isOverridden_returnsTrueWhenSubclassDeclaresMethod() {
        val origin = Parent::class.java.getDeclaredMethod("greet", String::class.java)
        assertTrue(invokeIsOverridden(Child::class.java, origin))
    }

    @Test
    fun isOverridden_returnsFalseWhenClassDoesNotOverride() {
        val origin = Parent::class.java.getDeclaredMethod("greet", String::class.java)
        assertFalse(invokeIsOverridden(Parent::class.java, origin))
    }

    @Test
    fun getOrigin_returnsOriginMethodWhenInstanceDoesNotOverride() {
        val advice =
            Advice(
                AndroidMockKMap(),
                AndroidMockKMap(),
                AndroidMockKMap(),
            )
        val origin = advice.getOrigin(Child(), "java.lang.Object#toString()")
        assertNotNull(origin)
        assertEquals("toString", origin!!.name)
    }

    @Test
    fun findMethod_returnsNullWhenMethodDoesNotExist() {
        assertNull(invokeFindMethod(String::class.java, "definitelyNotAMethod", emptyArray()))
    }

    @Test
    fun findMethod_returnsNullWhenLinkageErrorPreventsLookup() {
        val hostClass = loadHostWithTrapClass()
        assertNull(invokeFindMethod(hostClass, "safe", emptyArray()))
    }

    @Test
    fun declaredMethods_onHostWithTrap_failsWhenMissingTypeNotOnClasspath() {
        val hostClass = loadHostWithTrapClass()
        val error = assertThrows<NoClassDefFoundError> { hostClass.declaredMethods }
        assertTrue(
            error.message?.contains("MissingType") == true ||
                error.cause?.message?.contains("MissingType") == true,
        ) {
            "expected MissingType in NoClassDefFoundError, got: ${error.message}"
        }
    }

    private fun loadHostWithTrapClass(): Class<*> {
        val jarUrl =
            requireNotNull(javaClass.getResource("/poison-host-fixtures.jar")) {
                "poison-host-fixtures.jar missing from src/test/resources (see src/test/fixtures-src/README.md)"
            }
        return URLClassLoader(arrayOf(jarUrl), null)
            .loadClass("io.mockk.proxy.android.advice.fixtures.HostWithTrap")
    }

    private fun invokeIsOverridden(
        clazz: Class<*>,
        origin: Method,
    ): Boolean {
        val method =
            companionClass.getDeclaredMethod(
                "isOverridden",
                Class::class.java,
                Method::class.java,
            )
        method.isAccessible = true
        return method.invoke(companionInstance, clazz, origin) as Boolean
    }

    private fun invokeFindMethod(
        clazz: Class<*>,
        name: String,
        parameters: Array<Class<*>>,
    ): Method? {
        val method =
            companionClass.getDeclaredMethod(
                "findMethod",
                Class::class.java,
                String::class.java,
                emptyArray<Class<*>>().javaClass,
            )
        method.isAccessible = true
        return method.invoke(companionInstance, clazz, name, parameters) as Method?
    }

    private companion object {
        private val companionInstance =
            Advice::class.java
                .getDeclaredField("Companion")
                .apply { isAccessible = true }
                .get(null)
        private val companionClass = companionInstance.javaClass

        open class Parent {
            open fun greet(name: String) {
            }
        }

        class Child : Parent() {
            override fun greet(name: String) {
            }
        }
    }
}
