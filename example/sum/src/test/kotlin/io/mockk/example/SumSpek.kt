package io.mockk.example

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.Assert

class SumSpek : Spek({
    class Abc {
        fun sum(a: Int, b: Int) = a + b
    }

    describe("some mocked object") {
        val mock = mockk<SumJUnit4Test.Abc>()

        it("should return 5 as a result of mocking sum of 1 and 3") {
            every { mock.sum(1, 3) } returns 5

            Assert.assertEquals(5, mock.sum(1, 3))

            verify { mock.sum(1, 3) }
        }
    }
})