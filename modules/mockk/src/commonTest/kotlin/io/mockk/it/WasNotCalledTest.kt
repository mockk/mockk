package io.mockk.it

import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertFailsWith

class WasNotCalledTest {

  val mock = mockk<MockCls>()

  @Test
  fun wasNotCalledOnNonMockedObject() {
    assertFailsWith<MockKException>("was not can should throw MockKException on non mock object") {
      verify { mock.flowOp(1, 2) wasNot Called }
    }
  }

  @Test
  fun wasNotCalledOnMockedObject() {
    verify { mock wasNot Called }
  }

  @Test
  fun wasNotCalledShouldThrowAssertionErrorIfMockHasBeenCalled() {
    assertFailsWith<AssertionError>("was not can should throw AssertionError if mock has been called") {
      every { mock.flowOp(1, 2) } returns flowOf(1, 2)

      mock.flowOp(1, 2)
      verify { mock wasNot Called }
    }
  }

  class MockCls {
    fun flowOp(a: Int = 1, b: Int = 2) = flowOf(a, b)
  }
}
