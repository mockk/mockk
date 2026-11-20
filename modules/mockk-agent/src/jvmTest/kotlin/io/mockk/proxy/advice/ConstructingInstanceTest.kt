package io.mockk.proxy.advice

import io.mockk.proxy.MockKInvocationHandler
import io.mockk.proxy.jvm.advice.ConstructingInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.util.concurrent.Callable

class ConstructingInstanceTest {
    private val handler =
        object : MockKInvocationHandler {
            override fun invocation(
                self: Any,
                method: Method?,
                originalCall: Callable<*>?,
                args: Array<Any?>,
            ): Any? = null
        }

    private fun method(
        name: String,
        vararg params: Class<*>,
    ): Method = Object::class.java.getDeclaredMethod(name, *params)

    private fun declaredMethod(name: String): Method = Constructed::class.java.getDeclaredMethod(name)

    @Test
    fun `no constructing instance matches nothing`() {
        assertFalse(ConstructingInstance.matches(ConstructingInstanceTest::class.java))
        assertNull(ConstructingInstance.handlerFor(Constructed(), declaredMethod("userMethod")))
        assertNull(ConstructingInstance.handlerFor(null, declaredMethod("userMethod")))
    }

    @Test
    fun `instances of the tracked class match while construction is in progress`() {
        ConstructingInstance.during(Constructed::class.java, handler) {
            val instance = Constructed()
            assertTrue(ConstructingInstance.matches(instance))
            assertSame(handler, ConstructingInstance.handlerFor(instance, declaredMethod("userMethod")))
        }
    }

    @Test
    fun `state is cleared after construction completes`() {
        ConstructingInstance.during(Constructed::class.java, handler) {
            Constructed()
        }
        assertFalse(ConstructingInstance.matches(Constructed()))
    }

    @Test
    fun `subclasses of the tracked class match`() {
        ConstructingInstance.during(Constructed::class.java, handler) {
            assertTrue(ConstructingInstance.matches(SubConstructed()))
        }
    }

    @Test
    fun `unrelated instances do not match`() {
        ConstructingInstance.during(Constructed::class.java, handler) {
            assertFalse(ConstructingInstance.matches(Unrelated()))
            assertFalse(ConstructingInstance.matches(Constructed::class.java))
        }
    }

    @Test
    fun `methods declared by java lang Object are never routed`() {
        ConstructingInstance.during(Constructed::class.java, handler) {
            val instance = Constructed()
            assertNull(ConstructingInstance.handlerFor(instance, method("equals", Any::class.java)))
            assertNull(ConstructingInstance.handlerFor(instance, method("hashCode")))
            assertNull(ConstructingInstance.handlerFor(instance, method("toString")))
            assertNull(ConstructingInstance.handlerFor(instance, null))
        }
    }

    @Test
    fun `no window is opened for java lang Object`() {
        ConstructingInstance.during(Any::class.java, handler) {
            val instance = Constructed()
            assertFalse(ConstructingInstance.matches(instance))
            assertNull(ConstructingInstance.handlerFor(instance, declaredMethod("userMethod")))
        }
    }

    @Test
    fun `nested tracking restores the previous state`() {
        val outerHandler = handler
        val innerHandler =
            object : MockKInvocationHandler {
                override fun invocation(
                    self: Any,
                    method: Method?,
                    originalCall: Callable<*>?,
                    args: Array<Any?>,
                ): Any? = null
            }

        ConstructingInstance.during(Constructed::class.java, outerHandler) {
            ConstructingInstance.during(Unrelated::class.java, innerHandler) {
                assertSame(innerHandler, ConstructingInstance.handlerFor(Unrelated(), declaredMethod("userMethod")))
                assertNull(ConstructingInstance.handlerFor(Constructed(), declaredMethod("userMethod")))
            }
            assertSame(outerHandler, ConstructingInstance.handlerFor(Constructed(), declaredMethod("userMethod")))
            assertNull(ConstructingInstance.handlerFor(Unrelated(), declaredMethod("userMethod")))
        }
    }

    @Test
    fun `block result is returned and state is cleared on exception`() {
        val returned =
            ConstructingInstance.during(Constructed::class.java, handler) {
                Constructed()
                "result"
            }
        assertEquals("result", returned)

        try {
            ConstructingInstance.during(Constructed::class.java, handler) {
                throw IllegalStateException("boom")
            }
        } catch (ignored: IllegalStateException) {
        }
        assertFalse(ConstructingInstance.matches(Constructed()))
    }

    open class Constructed {
        fun userMethod() {}
    }

    class SubConstructed : Constructed()

    class Unrelated
}
