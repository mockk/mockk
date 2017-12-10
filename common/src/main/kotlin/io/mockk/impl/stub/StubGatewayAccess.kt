package io.mockk.impl.stub

import io.mockk.MockKGateway
import io.mockk.MockKGateway.CallRecorder
import io.mockk.impl.instantiation.AnyValueGenerator

data class StubGatewayAccess(val callRecorder: () -> CallRecorder,
                             val anyValueGenerator: AnyValueGenerator,
                             val mockFactory: MockKGateway.MockFactory? = null)