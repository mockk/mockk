package io.mockk.impl.recording

import io.mockk.MockKGateway.*
import io.mockk.impl.recording.states.CallRecordingState

typealias VerifierFactory = (VerificationParameters) -> CallVerifier
typealias SignatureMatcherDetectorFactory = () -> SignatureMatcherDetector
typealias CallRoundBuilderFactory = () -> CallRoundBuilder
typealias ChildHinterFactory = () -> ChildHinter

typealias PermanentMockerFactory = () -> PermanentMocker
typealias StateFactory = (recorder: CommonCallRecorder) -> CallRecordingState
typealias VerifyingStateFactory = (recorder: CommonCallRecorder, verificationParams: VerificationParameters) -> CallRecordingState
typealias ExclusionStateFactory = (recorder: CommonCallRecorder, exclusionParams: ExclusionParameters) -> CallRecordingState
typealias ChainedCallDetectorFactory = () -> ChainedCallDetector
typealias VerificationCallSorterFactory = () -> VerificationCallSorter

data class CallRecorderFactories(
    val signatureMatcherDetector: SignatureMatcherDetectorFactory,
    val callRoundBuilder: CallRoundBuilderFactory,
    val childHinter: ChildHinterFactory,
    val verifier: VerifierFactory,
    val permanentMocker: PermanentMockerFactory,
    val verificationCallSorter: VerificationCallSorterFactory,
    val answeringState: StateFactory,
    val stubbingState: StateFactory,
    val verifyingState: VerifyingStateFactory,
    val exclusionState: ExclusionStateFactory,
    val stubbingAwaitingAnswerState: StateFactory,
    val safeLoggingState: StateFactory
)
