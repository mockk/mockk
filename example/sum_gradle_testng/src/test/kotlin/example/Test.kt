package example

import io.mockk.every
import io.mockk.junit.MockKJUnit4Runner
import io.mockk.mockk
import io.mockk.testng.MockKObjectFactory
import io.mockk.verify
import org.testng.Assert
import org.testng.IObjectFactory
import org.testng.annotations.ObjectFactory
import org.testng.annotations.Test

class Test {
    val mock = mockk<MockedClass>()

    val testClass = TestClass(mock)

    @Test
    fun testResult() {
        every { mock.sum(5, 3) } returns 10

        Assert.assertEquals(10, testClass.result())

        verify { mock.sum(5, 3) }
    }

    class MockKClassTransformer {
        @ObjectFactory
        fun getObjectFactory(): IObjectFactory {
            return MockKObjectFactory()
        }
    }
}