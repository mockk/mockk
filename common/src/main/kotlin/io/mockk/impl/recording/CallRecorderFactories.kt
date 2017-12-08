package io.mockk.impl.recording

import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.Ordering
import io.mockk.Ref
import io.mockk.impl.recording.states.CallRecorderState

typealias VerifierFactory = (Ordering) -> CallVerifier
typealias SignatureMatcherDetectorFactory = (List<CallRound>, List<Ref>) -> SignatureMatcherDetector
typealias CallRoundBuilderFactory = () -> CallRoundBuilder
typealias ChildHinterFactory = () -> ChildHinter
typealias CallRecorderStateFactory = (recorder: CallRecorderImpl) -> CallRecorderState
typealias VerifyingCallRecorderStateFactory = (recorder: CallRecorderImpl, verificationParams: VerificationParameters) -> CallRecorderState
typealias ChainedCallDetectorFactory = (List<CallRound>, List<Ref>, Int) -> ChainedCallDetector

data class CallRecorderFactories(val signatureMatcherDetector: SignatureMatcherDetectorFactory,
                                 val callRoundBuilder: CallRoundBuilderFactory,
                                 val childHinter: ChildHinterFactory,
                                 val verifier: VerifierFactory,
                                 val answeringCallRecorderState: CallRecorderStateFactory,
                                 val stubbingCallRecorderState: CallRecorderStateFactory,
                                 val verifyingCallRecorderState: VerifyingCallRecorderStateFactory,
                                 val stubbingAwaitingAnswerCallRecorderState: CallRecorderStateFactory)

