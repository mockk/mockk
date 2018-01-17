package io.mockk.impl.recording

import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.Ordering
import io.mockk.impl.recording.states.CallRecordingState
import io.mockk.impl.stub.AdditionalAnswerOpportunity

typealias VerifierFactory = (Ordering) -> CallVerifier
typealias SignatureMatcherDetectorFactory = () -> SignatureMatcherDetector
typealias CallRoundBuilderFactory = () -> CallRoundBuilder
typealias ChildHinterFactory = () -> ChildHinter

typealias PermanentMockerFactory = () -> PermanentMocker
typealias StateFactory = (recorder: CommonCallRecorder) -> CallRecordingState
typealias VerifyingStateFactory = (recorder: CommonCallRecorder, verificationParams: VerificationParameters) -> CallRecordingState
typealias AnsweringStillAcceptingAnswersStateFactory = (recorder: CommonCallRecorder, answerOpportunity: AdditionalAnswerOpportunity) -> CallRecordingState
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
    val answeringStillAcceptingAnswersState: AnsweringStillAcceptingAnswersStateFactory,
    val stubbingState: StateFactory,
    val verifyingState: VerifyingStateFactory,
    val stubbingAwaitingAnswerState: StateFactory,
    val safeLoggingState: StateFactory
)
