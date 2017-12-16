package io.mockk.impl.recording.states

import io.mockk.Answer
import io.mockk.ConstantAnswer
import io.mockk.RecordedCall
import io.mockk.impl.every
import io.mockk.impl.log.LogLevel
import io.mockk.impl.log.Logger
import io.mockk.impl.mockk
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.verify
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
        recorder = mockk()
        state = StubbingAwaitingAnswerState(recorder)
        answer = mockk()
        call1 = mockk()
        call2 = mockk()
        obj1 = mockk()
        obj2 = mockk()
    }

    @Test
    fun givenAwaitingAnswerStateWhenAnswerCalledThenAnswerIsAddedAndSwitchAnsweringState() {
        every { recorder.calls } returns mutableListOf(call1, call2)
        every { call1.matcher.self } returns obj1
        every { call2.matcher.self } returns obj2
        every { call1.isRetValueMock } returns true
        every { recorder.factories.answeringCallRecorderState(recorder) } returns mockk()

        state.answer(answer)

        verify { recorder.factories.answeringCallRecorderState(recorder) }
        verify { recorder.stubRepo.stubFor(obj1).addAnswer(call1.matcher, ofType(ConstantAnswer::class)) }
        verify { recorder.stubRepo.stubFor(obj2).addAnswer(call2.matcher, answer) }
    }
}