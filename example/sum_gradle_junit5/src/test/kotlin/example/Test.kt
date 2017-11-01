package example

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class Test {
    val mock = mockk<MockedClass>()

    val testClass = TestClass(mock)

    @Test
    fun testResult() {
        every { mock.sum(5, 3) } returns 10

        Assertions.assertEquals(10, testClass.result())

        verify { mock.sum(5, 3) }
    }
}