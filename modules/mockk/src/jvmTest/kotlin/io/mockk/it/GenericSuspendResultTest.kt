package io.mockk.it

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class GenericSuspendResultTest {
    interface UseCase<I, O> {
        suspend operator fun invoke(input: I): Result<O>
    }

    class Implementation : UseCase<Unit, String> {
        override suspend fun invoke(input: Unit): Result<String> = Result.success("original")
    }

    @Test
    fun concreteMockCalledThroughGenericInterface() {
        val implementation = mockk<Implementation>()
        coEvery { implementation(Unit) } returns Result.success("stubbed")
        val useCase: UseCase<Unit, String> = implementation

        val value = runBlocking { useCase(Unit).getOrThrow() }

        assertEquals("stubbed", value)
        coVerify(exactly = 1) { implementation(Unit) }
    }

    @Test
    fun concreteMockCalledDirectly() {
        val implementation = mockk<Implementation>()
        coEvery { implementation(Unit) } returns Result.success("stubbed")

        assertEquals("stubbed", runBlocking { implementation(Unit).getOrThrow() })
    }

    @Test
    fun stubThroughInterfaceAnswersConcreteCall() {
        val implementation = mockk<Implementation>()
        val useCase: UseCase<Unit, String> = implementation
        coEvery { useCase(Unit) } returns Result.success("stubbed")

        assertEquals("stubbed", runBlocking { implementation(Unit).getOrThrow() })
        coVerify(exactly = 1) { useCase(Unit) }
    }

    @Test
    fun interfaceMockReturnsResult() {
        val useCase = mockk<UseCase<Unit, String>>()
        coEvery { useCase(Unit) } returns Result.success("stubbed")

        assertEquals("stubbed", runBlocking { useCase(Unit).getOrThrow() })
    }

    @Test
    fun failedResultThroughGenericInterfacePreservesException() {
        val implementation = mockk<Implementation>()
        val failure = IllegalStateException("failed")
        coEvery { implementation(Unit) } returns Result.failure(failure)
        val useCase: UseCase<Unit, String> = implementation

        assertSame(failure, runBlocking { useCase(Unit).exceptionOrNull() })
    }
}
