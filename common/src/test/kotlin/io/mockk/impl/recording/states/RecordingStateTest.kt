package io.mockk.impl.recording.states

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class RecordingStateTest {
    lateinit var recorder: CommonCallRecorder
    lateinit var state: RecordingState


    @BeforeTest
    fun setUp() {
        recorder = mockk(relaxed = true)
        state = object : RecordingState(recorder) {
        }
    }

    @Test
    fun givenRecordingStateWhenFirstCatchArgsHappensThenBuilderAndChildHinterInitialized() {
        every { recorder.factories.callRoundBuilder()  } just Runs
        every { recorder.factories.childHinter() } just Runs

        state.round(0, 1)

        verify { recorder.factories.callRoundBuilder() }
        verify { recorder.factories.childHinter() }
        verify { recorder.childHinter = any() }
    }

    @Test
    fun givenRecordingStateWhenLastCatchArgsHappensThenSignMatchersAndPermanentMockHappen() {
        every { recorder.factories.callRoundBuilder()  } just Runs
        every { recorder.factories.childHinter() } just Runs
        every { recorder.factories.signatureMatcherDetector().detect(any()) } just Runs
        every { recorder.factories.permanentMocker().mock(any()) } just Runs


        state.round(1, 1)

        verify { recorder.factories.signatureMatcherDetector().detect(any()) }
        verify { recorder.factories.permanentMocker().mock(any()) }
    }
}