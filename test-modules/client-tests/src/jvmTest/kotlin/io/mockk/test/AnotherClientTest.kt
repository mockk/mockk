package io.mockk.test

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AnotherClientTest {

    class Dependency1(val value1: Int)
    class Dependency2(val value2: String)

    class SystemUnderTest(
        private val dependency1: Dependency1,
        private val dependency2: Dependency2
    ) {
        fun calculate() =
            dependency1.value1 + dependency2.value2.toInt()
    }

    @Test
    fun calculateAddsValues() {
        val doc1 = mockk<Dependency1>()
        val doc2 = mockk<Dependency2>()

        every { doc1.value1 } returns 5
        every { doc2.value2 } returns "6"

        val sut = SystemUnderTest(doc1, doc2)

        assertEquals(11, sut.calculate())
    }
}
