package com.ninjasquad.springmockk

import io.mockk.MockK
import io.mockk.MockKGateway

/**
 * Utility extension function to check if an object is a MockK mock or spy
 */
val <T : Any> T.isMockOrSpy: Boolean
    get() = MockK.useImpl { MockKGateway.implementation().mockFactory.isMock(this) }
