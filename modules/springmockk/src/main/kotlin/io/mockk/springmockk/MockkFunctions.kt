package io.mockk.springmockk

import io.mockk.MockK
import io.mockk.MockKGateway

val <T: Any> T.isMock: Boolean
    get() = MockK.useImpl { MockKGateway.implementation().mockFactory.isMock(this) }
