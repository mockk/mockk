package io.mockk.impl.recording.states

import io.mockk.Answer
import io.mockk.ConstantAnswer
import io.mockk.MockKSettings
import io.mockk.RecordedCall
import io.mockk.every
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.stub.AnswerAnsweringOpportunity
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.assertThrows
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
        every { recorder.factories.answeringState(any()) } returns mockk()

        state.answerOpportunity()

        verify { recorder.stubRepo.stubFor(obj1).addAnswer(call1.matcher, ofType(ConstantAnswer::class)) }
        verify { recorder.stubRepo.stubFor(obj2).addAnswer(call2.matcher, ofType(AnswerAnsweringOpportunity::class)) }
        verify { recorder.factories.answeringState(any()) }
    }

    @Test
    fun `failOnSetBackingFieldException false just runs for invalid mock`() {
        val testContainerMock = mockk<TestContainer>()

        every { testContainerMock getProperty "someInt" } returns "mockValue"
    }

    @Test
    fun `failOnSetBackingFieldException true leads to exception for invalid mock`() {
        try {
            MockKSettings.setFailOnSetBackingFieldException(true)
            val testContainerMock = mockk<TestContainer>()
            assertThrows<IllegalArgumentException> {
                every { testContainerMock getProperty "someInt" } returns "mockValue"
            }
        } finally {
            // We reset the settings in the end to avoid side effects for other tests
            MockKSettings.setFailOnSetBackingFieldException(false)
        }
    }
}

private class TestContainer {
    @Suppress("unused") // Accessed via reflection
    val someInt = 5
}
