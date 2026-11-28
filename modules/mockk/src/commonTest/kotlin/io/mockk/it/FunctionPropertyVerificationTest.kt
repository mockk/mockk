package io.mockk.it

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.mockk
import io.mockk.verify
import kotlin.jvm.JvmName
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FunctionPropertyVerificationTest {
    private class Functions {
        val callback: (Int) -> Int = { it }

        @get:JvmName("callbackValue")
        val renamedCallback: (Int) -> Int = { it }

        fun getFactoryCallback(): (Int) -> Int = callback
    }

    @Test
    fun zeroInvocationsAllowsReadingFunctionProperty() {
        val mock = mockk<Functions>()
        every { mock.callback(any()) } returns 10

        val callbacks = listOf(mock.callback)

        verify(exactly = 0) { mock.callback(any()) }
        verify(exactly = 1) { mock.callback }
        assertEquals(10, callbacks.single()(1))
        verify(exactly = 1) { mock.callback(any()) }
        verify(exactly = 0) { mock.callback(2) }
    }

    @Test
    fun zeroInvocationsAllowsReadingPropertyWithRenamedGetter() {
        val mock = mockk<Functions>()
        every { mock.renamedCallback(any()) } returns 10

        listOf(mock.renamedCallback)

        verify(exactly = 0) { mock.renamedCallback(any()) }
    }

    @Test
    fun zeroInvocationsFailsAfterCallingFunctionFromProperty() {
        val mock = mockk<Functions>()
        every { mock.callback(any()) } returns 10

        assertEquals(10, mock.callback(1))

        val error =
            assertFailsWith<AssertionError> {
                verify(exactly = 0) { mock.callback(any()) }
            }
        assertContains(error.message.orEmpty().substringBefore('\n'), ".invoke(")
    }

    @Test
    fun explicitGetterVerificationStillChecksThatGetter() {
        val mock = mockk<Functions>()
        every { mock.callback(any()) } returns 10

        listOf(mock.callback)

        assertFailsWith<AssertionError> {
            verify(exactly = 0) {
                mock.callback
                mock.callback(any())
            }
        }
    }

    @Test
    fun zeroInvocationsFailsWhenOnlyTheGetterIsExcluded() {
        val mock = mockk<Functions>()
        every { mock.callback(any()) } returns 10
        excludeRecords { mock.callback }

        assertEquals(10, mock.callback(1))
        verify(exactly = 0) { mock.callback }
        val error =
            assertFailsWith<AssertionError> {
                verify(exactly = 0) { mock.callback(any()) }
            }
        assertContains(error.message.orEmpty().substringBefore('\n'), ".invoke(")
    }

    @Test
    fun verifiesFunctionFromAGetterRecordedAfterClearingCalls() {
        val mock = mockk<Functions>()
        every { mock.callback(any()) } returns 10
        listOf(mock.callback)
        clearMocks(mock, answers = false, childMocks = false)

        listOf(mock.callback)

        verify(exactly = 1) { mock.callback }
        verify(exactly = 0) { mock.callback(any()) }
    }

    @Test
    fun zeroInvocationsStillChecksAnOrdinaryMethodReturningAFunction() {
        val mock = mockk<Functions>()
        every { mock.getFactoryCallback()(any()) } returns 10

        listOf(mock.getFactoryCallback())

        val error =
            assertFailsWith<AssertionError> {
                verify(exactly = 0) { mock.getFactoryCallback()(any()) }
            }
        assertContains(error.message.orEmpty().substringBefore('\n'), ".getFactoryCallback(")
    }

    @Test
    fun zeroInvocationsDoesNotPassForAForeignFunctionMock() {
        val mock = mockk<Functions>()
        val callback = mockk<(Int) -> Int>()
        every { mock.callback } returns callback
        every { callback(1) } returns 10

        assertEquals(10, mock.callback(1))

        assertFailsWith<AssertionError> {
            verify(exactly = 0) { mock.callback(any()) }
        }
    }
}
