package io.mockk.proxy.util

import io.mockk.proxy.jvm.util.DefaultInterfaceMethodResolver
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import kotlin.test.Test


class DefaultInterfaceMethodResolverTest {

  interface A {
    fun method()
    fun defaultMethod(arg: String): String {
      return "Arg: $arg"
    }
  }

  class B : A {
    fun subclassMethod(arg: String) {}
    override fun method() {
    }
  }

  @Test
  fun `should return MethodCall when default implementation exists`() {
    val subclass = B()
    val method = A::class.java.getMethod("defaultMethod", String::class.java)
    val arguments = arrayOfNulls<Any>(1).also { it[0] = "arg" }

    val result = DefaultInterfaceMethodResolver.getDefaultImplementationOrNull(subclass, method, arguments)

    assertNotNull(result)

  }

  @Test
  fun `should return null when is concrete class method`() {
    val subclass = B()
    val method = B::class.java.getMethod("subclassMethod", String::class.java)
    val arguments = arrayOfNulls<Any>(1).also { it[0] = "arg" }

    val result = DefaultInterfaceMethodResolver.getDefaultImplementationOrNull(subclass, method, arguments)

    assertNull(result)
  }

  @Test
  fun `should return null when method is overwritten`() {
    val subclass = B()
    val method = A::class.java.getDeclaredMethod("method")
    val arguments = arrayOfNulls<Any>(0)

    val result = DefaultInterfaceMethodResolver.getDefaultImplementationOrNull(subclass, method, arguments)

    assertNull(result)
  }

  @Test
  fun `should return null when method is not a Kotlin class`() {
    val subclass = ArrayList<Any>()
    val method = ArrayList::class.java.getDeclaredMethod("add", Any::class.java)
    val arguments = arrayOfNulls<Any>(1).also { it[0] = "element" }

    val result = DefaultInterfaceMethodResolver.getDefaultImplementationOrNull(subclass, method, arguments)

    assertNull(result)
  }

}