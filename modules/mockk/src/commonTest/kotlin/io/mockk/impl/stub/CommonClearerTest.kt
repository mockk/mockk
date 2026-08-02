package io.mockk.impl.stub

import io.mockk.MockKGateway
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class CommonClearerTest {
    class Target

    private val options = MockKGateway.ClearOptions(true, true, true, true, true)

    private fun oneStubOfEachKind(): List<Stub> =
        MockType.values().map { mockType ->
            SpyKStub(Target::class, mockType.name, mockk(relaxed = true), false, mockType)
        }

    @Test
    fun `clearing every kind of mock reads the stub repository once`() {
        val stubRepository = mockk<StubRepository>(relaxed = true)
        every { stubRepository.allStubs } returns oneStubOfEachKind()

        CommonClearer(stubRepository, mockk(relaxed = true)).clearAll(
            options = options,
            currentThreadOnly = false,
            regularMocks = true,
            objectMocks = true,
            staticMocks = true,
            constructorMocks = true,
        )

        verify(exactly = 1) { stubRepository.allStubs }
    }

    @Test
    fun `clearing no kind of mock does not read the stub repository`() {
        val stubRepository = mockk<StubRepository>(relaxed = true)

        CommonClearer(stubRepository, mockk(relaxed = true)).clearAll(
            options = options,
            currentThreadOnly = false,
            regularMocks = false,
            objectMocks = false,
            staticMocks = false,
            constructorMocks = false,
        )

        verify(exactly = 0) { stubRepository.allStubs }
    }
}
