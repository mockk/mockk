package io.mockk.impl.stub

import io.mockk.*

class AnswerAnsweringOpportunity<T>(
    private val matcherStr: () -> String
) : MockKGateway.AnswerOpportunity<T>, Answer<T> {
    private var storedAnswer: Answer<T>? = null
    private val firstAnswerHandlers = mutableListOf<(Answer<T>) -> Unit>()

    private fun getAnswer() = storedAnswer ?: throw  MockKException("no answer provided for ${matcherStr()}")

    override fun answer(call: Call) = getAnswer().answer(call)
    override suspend fun coAnswer(call: Call) = getAnswer().answer(call)

    override fun provideAnswer(answer: Answer<T>) {
        synchronized(this) {
            val currentAnswer = this.storedAnswer
            this.storedAnswer = if (currentAnswer == null) {
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
        synchronized(this) {
            firstAnswerHandlers.add(handler)
        }
    }
}
