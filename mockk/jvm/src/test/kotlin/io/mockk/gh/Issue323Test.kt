package io.mockk.gh

import io.mockk.mockk
import io.mockk.verify
import org.junit.Ignore
import kotlin.test.Test

@Ignore("Temporarily ignored because it's failing only on travis and not anywhere else")
class Issue323Test {
    class MockedClass {
        fun test(s: String?) {
            println(s)
        }
    }

    class TestedClass(private val mockedClass: MockedClass) {
        fun test() {
            val testString: String? = null
            mockedClass.test(testString)
        }
    }

    @Test
    fun `withNullableArg matches and executes capture block when argument is null`() {
        val mock = mockk<MockedClass>(relaxed = true)
        val testedClass = TestedClass(mock)

        testedClass.test()

        verify(exactly = 1) {
            mock.test(withNullableArg { println(it) })
        }
    }
}
