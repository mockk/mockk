package io.mockk

/**
 * Returns one constant reply
 */
data class ConstantAnswer<T>(val constantValue: T?) : Answer<T?> {
    override fun answer(call: Call) = constantValue

    override fun toString(): String = "const($constantValue)"
}

/**
 * Delegates reply to the lambda function
 */
data class FunctionAnswer<T>(val answerFunc: (Call) -> T?) : Answer<T?> {
    override fun answer(call: Call): T? = answerFunc(call)

    override fun toString(): String = "answer()"
}

/**
 * Returns many different replies, each time moving the next list element.
 * Stops at the end.
 */
data class ManyAnswersAnswer<T>(val answers: List<T?>) : Answer<T?> {
    private var n = 0

    override fun answer(call: Call): T? {
        val next = if (n == answers.size - 1) n else n++
        return answers[next]
    }

}

/**
 * Throws exception instead of function reply
 */
data class ThrowingAnswer(val ex: Throwable) : Answer<Nothing> {
    override fun answer(call: Call): Nothing {
        throw ex
    }
}
