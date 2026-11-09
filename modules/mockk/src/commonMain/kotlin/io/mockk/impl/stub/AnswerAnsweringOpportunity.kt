package io.mockk.impl.stub

import io.mockk.Answer
import io.mockk.Call
import io.mockk.InvocationMatcher
import io.mockk.ManyAnswersAnswer
import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.impl.InternalPlatform

class AnswerAnsweringOpportunity<T>(
    private val stub: Stub? = null,
    private val matcher: InvocationMatcher? = null,
    private val matcherStr: () -> String,
) : MockKGateway.AnswerOpportunity<T>,
    Answer<T> {
    private var storedAnswer: Answer<T>? = null
    private val firstAnswerHandlers = mutableListOf<(Answer<T>) -> Unit>()

    private fun getAnswer() = storedAnswer ?: throw MockKException("no answer provided for ${matcherStr()}")

    override fun answer(call: Call) = getAnswer().answer(call)

    override suspend fun coAnswer(call: Call) = getAnswer().answer(call)

    override fun provideAnswer(answer: Answer<T>) {
        InternalPlatform.synchronized(this) {
            val currentAnswer = this.storedAnswer
            this.storedAnswer =
                if (currentAnswer == null) {
                    notifyFirstAnswerHandlers(answer)
                    answer
                } else {
                    ManyAnswersAnswer(listOf(currentAnswer, answer))
                }
        }
    }

    private fun notifyFirstAnswerHandlers(answer: Answer<T>) {
        firstAnswerHandlers.forEach { it(answer) }
    }

    fun onFirstAnswer(handler: (Answer<T>) -> Unit) {
        InternalPlatform.synchronized(this) {
            firstAnswerHandlers.add(handler)
        }
    }

    /**
     * Exempts the answer this opportunity is registered with from the unnecessary stubbing
     * check (issue #1272). Called by the stubbing evaluator when the stub was created inside
     * a `lenient { ... }` block.
     */
    fun markLenient() {
        val matcher = matcher ?: return
        stub?.markAnswerLenient(matcher)
    }
}
