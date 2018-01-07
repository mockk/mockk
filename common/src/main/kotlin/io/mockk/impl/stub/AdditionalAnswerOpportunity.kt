package io.mockk.impl.stub

import io.mockk.Answer
import io.mockk.ManyAnswersAnswer

class AdditionalAnswerOpportunity(val get: () -> Answer<*>, val set: (Answer<*>) -> Unit) {
    fun addAnswer(answer: Answer<*>) {
        set(combineAnswers(get(), answer))
    }

    private fun combineAnswers(firstAnswer: Answer<*>, secondAnswer: Answer<*>): Answer<*> {
        val lst = unMany(firstAnswer).toMutableList()
        lst.addAll(unMany(secondAnswer))
        return ManyAnswersAnswer(lst)
    }

    private fun unMany(firstAnswer: Answer<*>): List<Answer<*>> {
        return if (firstAnswer is ManyAnswersAnswer) {
            firstAnswer.answers
        } else {
            listOf(firstAnswer)
        }
    }
}