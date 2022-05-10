package io.mockk.junit5

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@MockKExtension.ConfirmVerification
class ConfirmVerificationExtensionTest {

    class MockCls {
        fun op(a: Int) = a + 1
    }


    @MockK
    private lateinit var mock: MockCls

    @Test
    fun automaticVerificationConfirmation() {
        every { mock.op(1) } returns 2

        assertEquals(2, mock.op(1))

        verify { mock.op(1) }
    }
}