package io.mockk

import kotlin.coroutines.Continuation
import kotlin.math.min

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
 * Delegates reply to the lambda suspendable function
 */
data class CoFunctionAnswer<T>(val answerFunc: suspend (Call) -> T) : Answer<T> {

    override fun answer(call: Call): T {
        val lastParam = call.invocation.args.lastOrNull()
        return if (lastParam is Continuation<*>)
            InternalPlatformDsl.coroutineCall {
                answerFunc(call)
            }.callWithContinuation(lastParam)
        else
            InternalPlatformDsl.runCoroutine {
                answerFunc(call)
            }
    }

    override suspend fun coAnswer(call: Call) = answerFunc(call)

    override fun toString(): String = "coAnswer()"

    companion object
}

/**
 * Required to signalize many answers available
 */
interface ManyAnswerable<out T> : Answer<T> {
    val hasMore: Boolean

    val flatAnswers: List<Answer<T>>
}

/**
 * Returns many different replies, each time moving the next list element.
 * Stops at the end.
 */
data class ManyAnswersAnswer<T>(val answers: List<Answer<T>>) : ManyAnswerable<T> {

    override val flatAnswers = answers.flatMap {
        when (it) {
            is ManyAnswerable<T> -> it.flatAnswers
            else -> listOf(it)
        }
    }

    private var pos = InternalPlatformDsl.counter()

    override val hasMore: Boolean
        get() = pos.value < flatAnswers.size

    override fun answer(call: Call): T {
        if (flatAnswers.isEmpty()) {
            throw MockKException("In many answers answer no answer available")
        }
        val pos = min(pos.increment(), (flatAnswers.size - 1).toLong()).toInt()
        return flatAnswers[pos].answer(call)
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
