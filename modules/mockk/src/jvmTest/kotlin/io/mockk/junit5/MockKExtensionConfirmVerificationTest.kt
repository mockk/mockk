package io.mockk.junit5

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.test.assertEquals

@MockKExtension.ConfirmVerification
@ExtendWith(MockKExtensionConfirmVerificationTest.LeaveUnverifiedCall::class, MockKExtension::class)
class MockKExtensionConfirmVerificationTest {
    @MockK
    lateinit var mock: Runnable

    @Test
    fun ignoresCallsRecordedBeforeTheClass() {
        every { mock.run() } returns Unit

        mock.run()

        verify { mock.run() }
    }

    // Acts like an earlier test class that mocked without MockKExtension and left a call unverified.
    class LeaveUnverifiedCall : BeforeAllCallback {
        override fun beforeAll(context: ExtensionContext) {
            val other = mockk<Callable>()
            every { other.call() } returns "mock"
            assertEquals("mock", other.call())
        }
    }

    interface Callable {
        fun call(): String
    }
}
