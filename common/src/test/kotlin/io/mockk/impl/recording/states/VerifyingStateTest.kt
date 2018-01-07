package io.mockk.impl.recording.states

import io.mockk.*
import io.mockk.MockKGateway.*
import io.mockk.impl.every
import io.mockk.impl.mockk
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.recording.VerificationCallSorter
import io.mockk.impl.spyk
import io.mockk.impl.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class VerifyingStateTest {
    lateinit var state: VerifyingState
    lateinit var recorder: CommonCallRecorder
    lateinit var verifier: CallVerifier
    lateinit var sorter: VerificationCallSorter
    lateinit var call1: RecordedCall
    lateinit var call2: RecordedCall

    @BeforeTest
    fun setUp() {
        recorder = mockk()
        val params = VerificationParameters(Ordering.UNORDERED, 1, 2, false)
        state = spyk(VerifyingState(recorder, params))
        verifier = mockk()
        sorter = mockk()
        call1 = mockk()
        call2 = mockk()
    }

    @Test
    fun givenEmptyCallListInVerifyingStateWhenRecordingDoneThenExceptionIsThrown() {
        every { recorder.calls.isEmpty() } returns true

        assertFailsWith<MockKException> {
            state.recordingDone()
        }
    }

    @Test
    fun givenCallsWithPositiveVerificationOutcomeWhenRecordingDoneThenSwitchToAnsweringState() {
        setupCalls(VerificationResult(true))
        setupWasNotCalled(0)

        state.recordingDone()

        verify {
            recorder.factories.answeringState(recorder)
        }
    }

    @Test
    fun givenCallsWithNegativeVerificationOutcomeWhenRecordingDoneThrowsException() {
        setupCalls(VerificationResult(false))
        setupWasNotCalled(0)

        assertFailsWith<AssertionError> {
            state.recordingDone()
        }
    }


    @Test
    fun givenOneCalledMocksWhenRecordingDoneThrowsException() {
        setupCalls(VerificationResult(true))
        setupWasNotCalled(1)

        assertFailsWith<AssertionError> {
            state.recordingDone()
        }
    }

    @Test
    fun givenTwoCalledMocksWhenRecordingDoneThrowsException() {
        setupCalls(VerificationResult(true))
        setupWasNotCalled(2)

        assertFailsWith<AssertionError> {
            state.recordingDone()
        }
    }

    private fun setupCalls(outcome: VerificationResult) {
        every { recorder.calls.isEmpty() } returns false
        every { recorder.factories.verifier(any()) } returns verifier
        every { recorder.factories.verificationCallSorter() } returns sorter
        every { sorter.regularCalls } returns listOf(call1, call2)
        every { sorter.sort(any()) } just Runs
        every { recorder.factories.answeringState(recorder) } returns mockk()
        every { recorder.safeExec<Any>(captureLambda()) } answers { lambda<() -> Any>().invoke() }
        every { verifier.verify(listOf(call1, call2), 1, 2) } returns outcome
    }

    private fun setupWasNotCalled(wasNotCalled: Int) {
        every { sorter.wasNotCalledCalls } returns listOf(call1, call2)

        when (wasNotCalled) {
            0 -> {
                every { allRecordedCalls(call1.matcher.self) } returns listOf()
                every { allRecordedCalls(call2.matcher.self) } returns listOf()
            }
            1 -> {
                every { allRecordedCalls(call1.matcher.self) } returns listOf(mockk())
                every { allRecordedCalls(call2.matcher.self) } returns listOf()
            }
            else -> {
                every { allRecordedCalls(call1.matcher.self) } returns listOf(mockk())
                every { allRecordedCalls(call2.matcher.self) } returns listOf(mockk())
            }
        }
    }

    private fun MockKMatcherScope.allRecordedCalls(mock: Any) = recorder.stubRepo.stubFor(mock).allRecordedCalls()
}