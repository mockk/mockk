package io.mockk.impl.platform

object JsHexLongHelper {
    val digits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

    fun toHexString(value: Long): String {
        val buf = StringBuilder()
        var v = value and 0xffffffffL
        do {
            buf.append(digits[v.toInt() and 15])
            v = v ushr 4
        } while (v != 0L)

        return buf.reverse().toString()
    }

}