package io.mockk.impl.stub

import io.mockk.MockKGateway
import io.mockk.MockKGateway.CallRecorder
import io.mockk.impl.instantiation.AnyValueGenerator
import io.mockk.impl.log.SafeToString

data class StubGatewayAccess(
    val callRecorder: () -> CallRecorder,
    val anyValueGenerator: () -> AnyValueGenerator,
    val stubRepository: StubRepository,
    val safeToString: SafeToString,
    val mockFactory: MockKGateway.MockFactory? = null
)
