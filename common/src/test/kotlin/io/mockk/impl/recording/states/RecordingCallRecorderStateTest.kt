package io.mockk.impl.recording.states

import io.mockk.Runs
import io.mockk.impl.every
import io.mockk.impl.mockk
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class RecordingCallRecorderStateTest {
    lateinit var recorder: CommonCallRecorder
    lateinit var state: RecordingCallRecorderState


    @BeforeTest
    fun setUp() {
        recorder = mockk()
        state = object : RecordingCallRecorderState(recorder) {
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
    fun givenRecordingStateWhenLastCatchArgsHappensThenSignMatchersAndMockRealChildsHappen() {
        every { recorder.factories.callRoundBuilder()  } just Runs
        every { recorder.factories.childHinter() } just Runs
        every { recorder.factories.signatureMatcherDetector(any(), any()).detect() } just Runs
        every { recorder.factories.realChildMocker(any(), any()).mock() } just Runs


        state.round(1, 1)

        verify { recorder.factories.signatureMatcherDetector(any(), any()).detect() }
        verify { recorder.factories.realChildMocker(any(), any()).mock() }
    }
}