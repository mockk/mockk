package io.mockk.impl.stub

import io.mockk.MockKGateway
import io.mockk.MockKGateway.CallRecorder
import io.mockk.impl.instantiation.AnyValueGenerator
import io.mockk.impl.log.SafeLog

data class StubGatewayAccess(
    val callRecorder: () -> CallRecorder,
    val anyValueGenerator: AnyValueGenerator,
    val stubRepository: StubRepository,
    val safeLog: SafeLog,
    val mockFactory: MockKGateway.MockFactory? = null
)
