package example

import io.mockk.every
import io.mockk.junit.MockKJUnit4Runner
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(MockKJUnit4Runner::class)
class Test {
    val mock = mockk<MockedClass>()

    val testClass = TestClass(mock)

    @Test
    fun testResult() {
        every { mock.sum(5, 3) } returns 10

        Assert.assertEquals(10, testClass.result())

        verify { mock.sum(5, 3) }
    }
}