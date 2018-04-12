import io.mockk.mockk
import io.mockk.every

class TestCls {
    fun op(a: Int, b: Int) = a + b
}

fun main(args: Array<String>) {

    val mockk = mockk<TestCls>()

    every { mockk.op(1, 2) } returns 4

    println(mockk.op(1, 2))
}