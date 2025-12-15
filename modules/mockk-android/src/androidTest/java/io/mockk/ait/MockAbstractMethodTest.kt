package io.mockk.ait

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.test.MinSdk
import io.mockk.test.MinSdkRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class MyClass: MyAbstractClass() {
    override fun foo(): String = "foo"
}

@RunWith(AndroidJUnit4::class)
class MockAbstractMethodTest {
    @get:Rule
    val minSdkRule = MinSdkRule()

    @Test
    @MinSdk(28) // Mocking final methods requires SDK 28+
    fun canMockAbstractMethodImplementation() {
        val myMock: MyClass = mockk()

        every { myMock.foo() } returns "bar"
    }
}
