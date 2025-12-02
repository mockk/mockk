package io.mockk.proxy.android

import java.lang.reflect.Method

internal class MethodDescriptor(
    signature: String
) {
    private val matchResult = methodPattern.matchEntire(signature)
            ?: throw IllegalArgumentException()

    val className = matchResult.groupValues[1]
    val methodName = matchResult.groupValues[2]
    val methodParamTypes = parseParamTypes(matchResult.groupValues[3])

    val method: Method
        get() = Class.forName(className)
            .getDeclaredMethod(
                methodName,
                *methodParamTypes
            )


    override fun toString() = "$className#$methodName"

    companion object {
        private val methodPattern = Regex("(.*)#(.*)\\((.*)\\)")

        /**
         * Parse parameter types from a comma-separated string.
         * This method avoids using Kotlin collection operations like dropLastWhile and filter
         * that internally call isEmpty() on collections, which could trigger the mock dispatcher
         * and cause infinite recursion when mocking classes that extend ArrayList or other collections.
         *
         * We use a two-pass approach:
         * 1. First pass: count the number of parameters
         * 2. Second pass: parse and store the parameter types in a pre-allocated array
         *
         * This avoids using mutable lists which could also trigger the dispatcher.
         */
        private fun parseParamTypes(paramString: String): Array<Class<*>> {
            if (paramString.isEmpty()) {
                return emptyArray()
            }

            val length = paramString.length

            // First pass: count the number of non-empty parameters
            var count = 0
            var i = 0

            while (i < length) {
                // Find the next comma
                var end = i
                while (end < length && paramString[end] != ',') {
                    end++
                }

                // Check if we have a non-empty parameter after trimming
                if (hasNonWhitespaceContent(paramString, i, end)) {
                    count++
                }

                i = end + 1
            }

            if (count == 0) {
                return emptyArray()
            }

            // Second pass: parse the parameters into a pre-allocated array
            // We use Array<Class<*>> constructor with an initializer to avoid unchecked casts
            val params = arrayOfNulls<Class<*>>(count)
            var paramIndex = 0
            i = 0

            while (i < length && paramIndex < count) {
                // Find the next comma
                var end = i
                while (end < length && paramString[end] != ',') {
                    end++
                }

                // Check if we have a non-empty parameter after trimming (same logic as first pass)
                if (hasNonWhitespaceContent(paramString, i, end)) {
                    val paramType = trimSubstring(paramString, i, end)
                    params[paramIndex++] = nameToType(paramType)
                }

                // Move past the comma
                i = end + 1
            }

            @Suppress("UNCHECKED_CAST")
            return params as Array<Class<*>>
        }

        /**
         * Check if substring has any non-whitespace content.
         * Avoids using String.trim() which may trigger collection operations.
         */
        private fun hasNonWhitespaceContent(str: String, start: Int, end: Int): Boolean {
            for (j in start until end) {
                if (str[j] != ' ') {
                    return true
                }
            }
            return false
        }

        /**
         * Trim whitespace from substring without using String.trim().
         * Returns the trimmed substring.
         */
        private fun trimSubstring(str: String, start: Int, end: Int): String {
            var trimStart = start
            var trimEnd = end

            // Skip leading whitespace
            while (trimStart < trimEnd && str[trimStart] == ' ') {
                trimStart++
            }

            // Skip trailing whitespace
            while (trimEnd > trimStart && str[trimEnd - 1] == ' ') {
                trimEnd--
            }

            return str.substring(trimStart, trimEnd)
        }

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
        ): String = if (n == 0) res else repeat(n - 1, str, res + str)
    }
}
