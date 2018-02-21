import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk

class Abc {
    fun y() = x()

    private fun x() = "abc"
}

fun main(args: Array<String>) {
    val mock = spyk<Abc>()
    every { mock["x"]() } returns "def"
    println(mock.y())
}