package io.mockk.impl.recording.states

import io.mockk.*
import io.mockk.impl.recording.CommonCallRecorder
import kotlin.test.BeforeTest
import kotlin.test.Test

class StubbingAwaitingAnswerStateTest {
    lateinit var recorder: CommonCallRecorder
    lateinit var state: StubbingAwaitingAnswerState
    lateinit var answer: Answer<*>
    lateinit var call1: RecordedCall
    lateinit var call2: RecordedCall
    lateinit var obj1: Any
    lateinit var obj2: Any

    @BeforeTest
    fun setUp() {
        recorder = mockk(relaxed = true)
        state = StubbingAwaitingAnswerState(recorder)
        answer = mockk(relaxed = true)
        call1 = mockk(relaxed = true)
        call2 = mockk(relaxed = true)
        obj1 = mockk(relaxed = true)
        obj2 = mockk(relaxed = true)
    }

    @Test
    fun givenAwaitingAnswerStateWhenAnswerCalledThenAnswerIsAddedAndSwitchAnsweringState() {
        every { recorder.calls } returns mutableListOf(call1, call2)
        every { call1.matcher.self } returns obj1
        every { call2.matcher.self } returns obj2
        every { call1.isRetValueMock } returns true
        every { recorder.factories.answeringStillAcceptingAnswersState(recorder, any()) } returns mockk(relaxed = true)

        state.answer(answer)

        verify { recorder.factories.answeringStillAcceptingAnswersState(recorder, any()) }
        verify { recorder.stubRepo.stubFor(obj1).addAnswer(call1.matcher, ofType(ConstantAnswer::class)) }
        verify { recorder.stubRepo.stubFor(obj2).addAnswer(call2.matcher, answer) }
    }
}