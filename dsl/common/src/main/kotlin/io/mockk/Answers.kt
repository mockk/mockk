package io.mockk

/**
 * Returns one constant reply
 */
data class ConstantAnswer<T>(val constantValue: T) : Answer<T> {
    override fun answer(call: Call) = constantValue

    override fun toString(): String = "const($constantValue)"
}

/**
 * Delegates reply to the lambda function
 */
data class FunctionAnswer<T>(val answerFunc: (Call) -> T) : Answer<T> {
    override fun answer(call: Call): T = answerFunc(call)

    override fun toString(): String = "answer()"
}

/**
 * Allows to check if has one more element in answer
 */
interface ManyAnswerable<out T> : Answer<T> {
    val hasMore: Boolean
}

/**
 * Returns many different replies, each time moving the next list element.
 * Stops at the end.
 */
data class ManyAnswersAnswer<T>(val answers: List<Answer<T>>) : ManyAnswerable<T> {
    private var n = 0
    private var prevAnswer: Answer<T>? = null
    val manyAnswers = answers.map { if (it is ManyAnswerable) it else SingleAnswer(it) }

    inner class SingleAnswer(val wrapped: Answer<T>) : ManyAnswerable<T> {
        var answered = false

        override val hasMore: Boolean
            get() = !answered

        override fun answer(call: Call): T {
            answered = true
            return wrapped.answer(call)
        }
    }

    private fun nextAnswerable(): ManyAnswerable<T>? {
        while (n < answers.size) {
            if (manyAnswers[n].hasMore) {
                return manyAnswers[n]
            }
            prevAnswer = manyAnswers[n]
            n++
        }
        return null
    }

    override val hasMore: Boolean
        get() = nextAnswerable()?.hasMore ?: false


    override fun answer(call: Call): T {
        val next = nextAnswerable()
        if (next != null) {
            return next.answer(call)
        }
        val prev = prevAnswer
        if (prev != null) {
            return prev.answer(call)
        }
        throw RuntimeException("In many answers answer no answer available")
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
