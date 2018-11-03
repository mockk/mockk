package io.mockk.impl.stub

import io.mockk.Answer
import io.mockk.ManyAnswersAnswer

class AdditionalAnswerOpportunity(val get: () -> Answer<*>, val set: (Answer<*>) -> Unit) {
    fun addAnswer(answer: Answer<*>) {
        set(ManyAnswersAnswer(listOf(get(), answer)))
    }
}