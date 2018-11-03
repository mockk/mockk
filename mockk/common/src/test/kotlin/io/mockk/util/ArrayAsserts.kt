package io.mockk.util

import kotlin.math.abs
import kotlin.test.assertTrue

fun assertArrayEquals(actual: BooleanArray, expected: BooleanArray) {
    assertTrue(actual contentEquals expected)
}

fun assertArrayEquals(actual: ByteArray, expected: ByteArray) {
    assertTrue(actual contentEquals expected)
}

fun assertArrayEquals(actual: ShortArray, expected: ShortArray) {
    assertTrue(actual contentEquals expected)
}

fun assertArrayEquals(actual: CharArray, expected: CharArray) {
    assertTrue(actual contentEquals expected)
}

fun assertArrayEquals(actual: IntArray, expected: IntArray) {
    assertTrue(actual contentEquals expected)
}

fun assertArrayEquals(actual: LongArray, expected: LongArray) {
    assertTrue(actual contentEquals expected)
}

fun assertArrayEquals(actual: FloatArray, expected: FloatArray, prec: Float) {
    assertTrue(actual.size == expected.size &&
            !actual.zip(expected)
                .any { (a, b) -> abs(a - b) > prec })
}

fun assertArrayEquals(actual: DoubleArray, expected: DoubleArray, prec: Double) {
    assertTrue(actual.size == expected.size &&
            !actual.zip(expected)
                .any { (a, b) -> abs(a - b) > prec })
}

fun assertArrayEquals(actual: Array<*>, expected: Array<*>) {
    assertTrue(actual contentDeepEquals expected)
}