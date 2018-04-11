import io.mockk.MockKDsl

class Test {
    fun op(a: Int, b: Int) = a + b
}

fun main(args: Array<String>) {
    val mockk = MockKDsl.internalMockk<Test>()

    MockKDsl.internalEvery { mockk.op(1, 2) } returns 4

    println(mockk.op(1, 2))
}