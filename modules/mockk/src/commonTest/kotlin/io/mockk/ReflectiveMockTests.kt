@file:Suppress("UNCHECKED_CAST")

package io.mockk

import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.memberFunctions
import kotlin.test.Test


class ReflectiveMockTests {
  private inline fun <reified T : Any> mockkBuilder(): T {
    val builder: T = mockk()
    builder.javaClass.kotlin.memberFunctions
      .filter { it.returnType.classifier == T::class }
      .filter { !it.isSuspend }
      .forEach { func ->
        every {
          val params: List<Any?> =
            listOf<Any?>(builder) + func.parameters.drop(1).map { any(it.type.classifier as KClass<Any>) }
          func.call(*params.toTypedArray())
        } answers { builder }
      }
    return builder
  }

  interface TestBuilder {
    fun f1(p1: String): TestBuilder
    fun f2(p1: String, p2: Int): TestBuilder
    fun f3(p1: String, p2: Int, p3: Long?): TestBuilder
  }

  @Test
  fun testAQuickMockBuilder() {
    val builder: TestBuilder = mockkBuilder()

    builder.f1("f1")
      .f2("f2", 2)
      .f3("f3", 3, 3)

    verify {
      builder.f1("f1")
      builder.f2("f2", 2)
      builder.f3("f3", 3, 3)
    }
  }


  private inline fun <reified T : Any> mockkWithDefault(noinline defaultAnswer: MockKAnswerScope<Any?, Any?>.(Call) -> Any?): T {
    val mock: T = mockk()
    mock.javaClass.kotlin.memberFunctions
      .filter { !it.isSuspend }
      .forEach { func ->
        every {
          val params: List<Any?> =
            listOf<Any?>(mock) + func.parameters.drop(1).map { any(it.type.classifier as KClass<Any>) }
          func.call(*params.toTypedArray())
        } answers {
          this.defaultAnswer(it)
        }
      }

    mock.javaClass.kotlin.memberFunctions
      .filter { it.isSuspend }
      .forEach { func ->
        coEvery {
          val params: List<Any?> =
            listOf<Any?>(mock) + func.parameters.drop(1).map { any(it.type.classifier as KClass<Any>) }
          func.callSuspend(*params.toTypedArray())
        } answers {
          this.defaultAnswer(it)
        }
      }
    return mock
  }

  @Test fun testMockkWithDefault() {
    val builder: TestBuilder = mockkWithDefault {
      when (it.retType) {
        TestBuilder::class -> it.invocation.self
        Unit::class        -> Unit
        else               -> null
      }
    }

    builder.f1("f1")
      .f2("f2", 2)
      .f3("f3", 3, 3)

    verify {
      builder.f1("f1")
      builder.f2("f2", 2)
      builder.f3("f3", 3, 3)
    }
  }
}
