package io.mockk.ait

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith

class MyClass: MyAbstractClass() {
    override fun foo(): String = "foo"
}

@RunWith(AndroidJUnit4::class)
class MockAbstractMethodTest {

    @Test
    fun canMockAbstractMethodImplementation() {
        val myMock: MyClass = mockk()

        every { myMock.foo() } returns "bar"
    }
}
