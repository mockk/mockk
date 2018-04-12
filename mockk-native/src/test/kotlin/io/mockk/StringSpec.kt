@file:Suppress("DEPRECATION")

package io.mockk

import kotlin.browser.document
import kotlin.dom.appendText
import kotlin.js.Math

open class StringSpec(block: StringSpec.() -> Unit) {
    data class Test(val name: String, val block: () -> Unit)

    val tests = mutableListOf<Test>()
    operator fun String.invoke(block: () -> Unit) = tests.add(Test(this, block))

    init {
        var nTest = 0
        while (true) {
            tests.clear()
            block()
            val test = tests[nTest++]
            try {
                test.block()
                report("[+] " + test.name)
            } catch (ex: AssertionError) {
                report("[-] " + test.name + " : failure")
                console.log(js("ex.stack"))
                break
            } catch (ex: Throwable) {
                report("[x] " + test.name + " : exception")
                console.log(js("ex.stack"))
                break
            }
            if (nTest >= tests.size) {
                break
            }
        }
    }

    private fun report(str: String) {
        val report = document.getElementById("report")
        report?.appendText(str + "\n")
    }

    fun fail(msg: String): Nothing = throw AssertionError(msg)

    fun assertTrue(condition: Boolean) {
        if (!condition) {
            fail("Not true")
        }
    }

    fun assertNull(actual: Any?) {
        if (actual != null) {
            fail("Not null")
        }
    }

    fun assertNotNull(actual: Any?) {
        if (actual == null) {
            fail("Null")
        }
    }

    fun assertEquals(expected: Double, actual: Double, precession: Double) {
        if (Math.abs(expected - actual) >= precession) {
            fail("expected [$expected] != actual [$actual]")
        }
    }

    fun assertEquals(expected: Any?, actual: Any?) {
        if (expected is BooleanArray && actual is BooleanArray) {
            return assertArrayEquals(expected, actual)
        } else if (expected is Array<*> && actual is Array<*>) {
            return assertArrayEquals(expected, actual)
        }
        if (expected != actual) {
            fail("expected [$expected] != actual [$actual]")
        }
    }

    fun assertArrayEquals(expected: Array<*>, actual: Array<*>) {
        if (!(expected contentDeepEquals actual)) {
            fail("expected [${expected.contentDeepToString()}] != actual [${actual.contentDeepToString()}]")
        }
    }

    fun assertArrayEquals(expected: BooleanArray, actual: BooleanArray) =
        failIfFalse(
            expected contentEquals actual,
            expected.contentToString(), actual.contentToString()
        )

    fun assertArrayEquals(expected: ByteArray, actual: ByteArray) =
        failIfFalse(
            expected contentEquals actual,
            expected.contentToString(), actual.contentToString()
        )

    fun assertArrayEquals(expected: CharArray, actual: CharArray) =
        failIfFalse(
            expected contentEquals actual,
            expected.contentToString(), actual.contentToString()
        )

    fun assertArrayEquals(expected: ShortArray, actual: ShortArray) =
        failIfFalse(
            expected contentEquals actual,
            expected.contentToString(), actual.contentToString()
        )

    fun assertArrayEquals(expected: IntArray, actual: IntArray) =
        failIfFalse(
            expected contentEquals actual,
            expected.contentToString(), actual.contentToString()
        )

    fun assertArrayEquals(expected: LongArray, actual: LongArray) =
        failIfFalse(
            expected contentEquals actual,
            expected.contentToString(), actual.contentToString()
        )

    fun assertArrayEquals(expected: FloatArray, actual: FloatArray, precession: Float) =
        failIfFalse(
            expected contentEquals actual,
            expected.contentToString(), actual.contentToString()
        )

    fun assertArrayEquals(expected: DoubleArray, actual: DoubleArray, precession: Double) =
        failIfFalse(
            expected contentEquals actual,
            expected.contentToString(), actual.contentToString()
        )

    private fun failIfFalse(cond: Boolean, expected: String, actual: String) {
        if (!cond) {
            fail("expected [$expected] != actual [$actual]")
        }
    }

}
