package io.mockk
import kotlin.js.Math

open class StringSpec(block: StringSpec.() -> Unit) {
    data class Test(val name: String, val block: () -> Unit)

    val tests = mutableListOf<Test>()
    operator fun String.invoke(block: () -> Unit) = tests.add(Test(this, block))

    init {
        block()
        for (test in tests) {
            print(test.name + " [")
            try {
                test.block()
                println("+]")
            } catch (ex: AssertionError) {
                println("-]: " + ex.message)
            } catch (ex: Throwable) {
                println("x]: " + ex.toString())
            }
        }
    }

    fun fail(msg: String): Nothing = throw AssertionError(msg)

    fun assertEquals(expected: Double, actual: Double, precession: Double) {
        if (Math.abs(expected - actual) >= precession) {
            fail("expected [$expected] != actual [$actual]")
        }
    }

    fun assertEquals(expected: Any?, actual: Any?) {
        if (expected != actual) {
            fail("expected [$expected] != actual [$actual]")
        }
    }

    fun <T> assertArrayEquals(expected: Array<T>, actual: Array<T>) {
        if (expected contentDeepEquals actual) {
            fail("expected [${expected.contentDeepToString()}] != actual [${actual.contentDeepToString()}]")
        }
    }

    fun assertArrayEquals(expected: BooleanArray, actual: BooleanArray) =
            failIfFalse(expected contentEquals actual,
                    expected.contentToString(), actual.contentToString())

    fun assertArrayEquals(expected: ByteArray, actual: ByteArray) =
            failIfFalse(expected contentEquals actual,
                    expected.contentToString(), actual.contentToString())

    fun assertArrayEquals(expected: CharArray, actual: CharArray) =
            failIfFalse(expected contentEquals actual,
                    expected.contentToString(), actual.contentToString())

    fun assertArrayEquals(expected: ShortArray, actual: ShortArray) =
            failIfFalse(expected contentEquals actual,
                    expected.contentToString(), actual.contentToString())

    fun assertArrayEquals(expected: IntArray, actual: IntArray) =
            failIfFalse(expected contentEquals actual,
                    expected.contentToString(), actual.contentToString())

    fun assertArrayEquals(expected: LongArray, actual: LongArray) =
            failIfFalse(expected contentEquals actual,
                    expected.contentToString(), actual.contentToString())

    fun assertArrayEquals(expected: FloatArray, actual: FloatArray, precession: Float) =
            failIfFalse(expected contentEquals actual,
                    expected.contentToString(), actual.contentToString())

    fun assertArrayEquals(expected: DoubleArray, actual: DoubleArray, precession: Double) =
            failIfFalse(expected contentEquals actual,
                    expected.contentToString(), actual.contentToString())

    private fun failIfFalse(cond: Boolean, expected: String, actual: String) {
        if (cond) {
            fail("expected [$expected] != actual [$actual]")
        }
    }

}
