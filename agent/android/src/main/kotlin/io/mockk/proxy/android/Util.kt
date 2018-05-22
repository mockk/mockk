package io.mockk.proxy.android

internal object Util {
    fun nameToType(name: String): Class<*> {
        when (name) {
            "byte" -> return java.lang.Byte.TYPE
            "short" -> return java.lang.Short.TYPE
            "int" -> return Integer.TYPE
            "long" -> return java.lang.Long.TYPE
            "char" -> return Character.TYPE
            "float" -> return java.lang.Float.TYPE
            "double" -> return java.lang.Double.TYPE
            "boolean" -> return java.lang.Boolean.TYPE
            "byte[]" -> return ByteArray::class.java
            "short[]" -> return ShortArray::class.java
            "int[]" -> return IntArray::class.java
            "long[]" -> return LongArray::class.java
            "char[]" -> return CharArray::class.java
            "float[]" -> return FloatArray::class.java
            "double[]" -> return DoubleArray::class.java
            "boolean[]" -> return BooleanArray::class.java
            else -> return classForTypeName(name)
        }
    }

    fun classForTypeName(name: String): Class<*> {
        val (baseType, nArrays) = numberOfSquareBrackets(name)

        val className = when {
            nArrays > 0 ->
                repeat(nArrays, "[") + "L" + baseType + ";"
            else ->
                baseType
        }

        return Class.forName(className)
    }

    private tailrec fun numberOfSquareBrackets(
        name: String,
        nPairs: Int = 0
    ): Pair<String, Int> =
        when {
            name.endsWith("[]") ->
                numberOfSquareBrackets(
                    name.substring(0, name.length - 2),
                    nPairs + 1
                )
            else ->
                name to nPairs
        }

    private tailrec fun repeat(
        n: Int,
        str: String,
        res: String = ""
    ): String =
        when (n) {
            0 -> res
            else -> repeat(n, str, res + str)
        }
}
