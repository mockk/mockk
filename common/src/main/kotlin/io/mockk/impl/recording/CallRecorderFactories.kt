package io.mockk.impl.recording

import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.Ordering
import io.mockk.impl.recording.states.CallRecordingState

typealias VerifierFactory = (Ordering) -> CallVerifier
typealias SignatureMatcherDetectorFactory = () -> SignatureMatcherDetector
typealias CallRoundBuilderFactory = () -> CallRoundBuilder
typealias ChildHinterFactory = () -> ChildHinter

typealias PermanentMockerFactory = () -> PermanentMocker
typealias StateFactory = (recorder: CommonCallRecorder) -> CallRecordingState
typealias VerifyingStateFactory = (recorder: CommonCallRecorder, verificationParams: VerificationParameters) -> CallRecordingState
typealias ChainedCallDetectorFactory = () -> ChainedCallDetector
typealias VerificationCallSorterFactory = () -> VerificationCallSorter

data class CallRecorderFactories(val signatureMatcherDetector: SignatureMatcherDetectorFactory,
                                 val callRoundBuilder: CallRoundBuilderFactory,
                                 val childHinter: ChildHinterFactory,
                                 val verifier: VerifierFactory,
                                 val permanentMocker: PermanentMockerFactory,
                                 val verificationCallSorter: VerificationCallSorterFactory,
                                 val answeringCallRecorderState: StateFactory,
                                 val stubbingCallRecorderState: StateFactory,
                                 val verifyingCallRecorderState: VerifyingStateFactory,
                                 val stubbingAwaitingAnswerCallRecorderState: StateFactory,
                                 val safeLoggingState: StateFactory)
