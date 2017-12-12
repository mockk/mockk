package io.mockk.impl.recording

import io.mockk.MatchedCall
import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.Ordering
import io.mockk.Ref
import io.mockk.impl.recording.states.CallRecorderState
import io.mockk.impl.stub.StubRepository

typealias VerifierFactory = (Ordering) -> CallVerifier
typealias SignatureMatcherDetectorFactory = () -> SignatureMatcherDetector
typealias CallRoundBuilderFactory = () -> CallRoundBuilder
typealias ChildHinterFactory = () -> ChildHinter
typealias CallRecorderStateFactory = (recorder: CommonCallRecorder) -> CallRecorderState
typealias VerifyingCallRecorderStateFactory = (recorder: CommonCallRecorder, verificationParams: VerificationParameters) -> CallRecorderState
typealias ChainedCallDetectorFactory = () -> ChainedCallDetector
typealias RealChildMockerFactory = () -> RealChildMocker

data class CallRecorderFactories(val signatureMatcherDetector: SignatureMatcherDetectorFactory,
                                 val callRoundBuilder: CallRoundBuilderFactory,
                                 val childHinter: ChildHinterFactory,
                                 val verifier: VerifierFactory,
                                 val answeringCallRecorderState: CallRecorderStateFactory,
                                 val stubbingCallRecorderState: CallRecorderStateFactory,
                                 val verifyingCallRecorderState: VerifyingCallRecorderStateFactory,
                                 val stubbingAwaitingAnswerCallRecorderState: CallRecorderStateFactory,
                                 val realChildMocker: RealChildMockerFactory)

