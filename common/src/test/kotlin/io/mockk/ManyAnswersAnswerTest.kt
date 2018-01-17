package io.mockk

import kotlin.test.Test
import kotlin.test.assertEquals

class ManyAnswersAnswerTest {
    @Test
    fun simpleList() {
        val many = many(1, 2, 3)

        assertEquals(listOf(1, 2, 3), many.toList())
    }

    @Test
    fun nestedList() {
        val many = many(many(1, 2, 3), many(4, 5, 6))

        assertEquals(listOf(1, 2, 3, 4, 5, 6), many.toList())
    }

    @Test
    fun mixedList() {
        val many = many(
            const(0),
            many(1, 2, 3),
            const(4),
            many(5, 6, 7),
            const(8)
        )

        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), many.toList())
    }

    private fun <T> many(vararg args: T) = many(*args.map { const(it) }.toTypedArray())
    private fun <T> many(vararg args: Answer<T>) = ManyAnswersAnswer(listOf(*args))
    private fun <T> const(value: T) = ConstantAnswer(value)

    private fun <T> ManyAnswersAnswer<T>.toList(): List<T> {
        val res = mutableListOf<T>()
        while (hasMore) {
            res.add(answer(mockk()))
        }
        return res.toList()
    }
}

