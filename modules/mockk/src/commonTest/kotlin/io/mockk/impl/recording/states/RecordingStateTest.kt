package io.mockk.impl.recording.states

import io.mockk.every
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class RecordingStateTest {
    private lateinit var recorder: CommonCallRecorder
    private lateinit var state: RecordingState

    @BeforeTest
    fun setUp() {
        recorder = mockk(relaxed = true)
        state = object : RecordingState(recorder) {
        }
    }

    @Test
    fun givenRecordingStateWhenFirstCatchArgsHappensThenBuilderAndChildHinterInitialized() {
        every { recorder.factories.callRoundBuilder() } returns mockk()
        every { recorder.factories.childHinter() } returns mockk()

        state.round(0, 1)

        verify { recorder.factories.callRoundBuilder() }
        verify { recorder.factories.childHinter() }
        verify { recorder.childHinter = any() }
    }

    @Test
    fun givenRecordingStateWhenLastCatchArgsHappensThenSignMatchersAndPermanentMockHappen() {
        every { recorder.factories.callRoundBuilder() } returns mockk()
        every { recorder.factories.childHinter() } returns mockk()
        every { recorder.factories.signatureMatcherDetector().detect(any()) } returns mockk()
        every { recorder.factories.permanentMocker().mock(any()) } returns mockk(relaxed = true)

        state.round(1, 1)

        verify { recorder.factories.signatureMatcherDetector().detect(any()) }
        verify { recorder.factories.permanentMocker().mock(any()) }
    }
}
