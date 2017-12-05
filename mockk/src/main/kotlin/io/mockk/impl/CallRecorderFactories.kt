package io.mockk.impl

import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.Ordering
import io.mockk.Ref

internal typealias VerifierFactory = (Ordering) -> CallVerifier
internal typealias SignatureMatcherDetectorFactory = (List<CallRound>, List<Ref>) -> SignatureMatcherDetector
internal typealias CallRoundBuilderFactory = () -> CallRoundBuilder
internal typealias ChildHinterFactory = () -> ChildHinter
internal typealias CallRecorderStateFactory = (recorder: CallRecorderImpl) -> CallRecorderState
internal typealias VerifyingCallRecorderStateFactory = (recorder: CallRecorderImpl, verificationParams: VerificationParameters) -> CallRecorderState

internal data class CallRecorderFactories(val signatureMatcherDetector: SignatureMatcherDetectorFactory,
                                          val callRoundBuilder: CallRoundBuilderFactory,
                                          val childHinter: ChildHinterFactory,
                                          val verifier: VerifierFactory,
                                          val answeringCallRecorderState: CallRecorderStateFactory,
                                          val stubbingCallRecorderState: CallRecorderStateFactory,
                                          val verifyingCallRecorderState: VerifyingCallRecorderStateFactory,
                                          val stubbingAwaitingAnswerCallRecorderState: CallRecorderStateFactory)

