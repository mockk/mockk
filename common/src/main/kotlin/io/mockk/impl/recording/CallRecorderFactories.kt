package io.mockk.impl.recording

import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.Ordering
import io.mockk.impl.recording.states.CallRecorderState

typealias VerifierFactory = (Ordering) -> CallVerifier
typealias SignatureMatcherDetectorFactory = () -> SignatureMatcherDetector
typealias CallRoundBuilderFactory = () -> CallRoundBuilder
typealias ChildHinterFactory = () -> ChildHinter

typealias PermanentMockerFactory = () -> PermanentMocker
typealias CallRecorderStateFactory = (recorder: CommonCallRecorder) -> CallRecorderState
typealias VerifyingCallRecorderStateFactory = (recorder: CommonCallRecorder, verificationParams: VerificationParameters) -> CallRecorderState
typealias ChainedCallDetectorFactory = () -> ChainedCallDetector
typealias VerificationCallSorterFactory = () -> VerificationCallSorter

data class CallRecorderFactories(val signatureMatcherDetector: SignatureMatcherDetectorFactory,
                                 val callRoundBuilder: CallRoundBuilderFactory,
                                 val childHinter: ChildHinterFactory,
                                 val verifier: VerifierFactory,
                                 val permanentMocker: PermanentMockerFactory,
                                 val verificationCallSorter: VerificationCallSorterFactory,
                                 val answeringCallRecorderState: CallRecorderStateFactory,
                                 val stubbingCallRecorderState: CallRecorderStateFactory,
                                 val verifyingCallRecorderState: VerifyingCallRecorderStateFactory,
                                 val stubbingAwaitingAnswerCallRecorderState: CallRecorderStateFactory,
                                 val safeLoggingState: CallRecorderStateFactory)
