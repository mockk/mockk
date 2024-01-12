package io.mockk.it

import io.mockk.*
import kotlin.test.Test

class CallOriginalOnDefaultInterfaceMethodTest {

  interface A {
    fun method1(items: List<Int>)
    fun method2(items: List<Int>)
    fun defaultMethod(callMethod2: Boolean) {
      method1(listOf(1, 2, 3))
      if (callMethod2)
        method2(listOf(4, 5, 6))
    }
  }

  @Test
  fun `should call the original default method when spy the class`() {
    val spy = spyk<A>()
    every { spy.defaultMethod(any()) } answers { callOriginal() }

    spy.defaultMethod(callMethod2 = true)

    verify { spy.method1(listOf(1, 2, 3)) }
    verify { spy.method2(listOf(4, 5, 6)) }
  }

  @Test
  fun `should call the original default method when mock the class`() {
    val mock = mockk<A>()
    every { mock.defaultMethod(any()) } answers { callOriginal() }
    every { mock.method1(any()) } just runs
    every { mock.method2(any()) } just runs

    mock.defaultMethod(callMethod2 = true)

    verify { mock.method1(listOf(1, 2, 3)) }
    verify { mock.method2(listOf(4, 5, 6)) }
  }

}
