package io.mockk.junit5

import io.mockk.isMockKMock
import io.mockk.junit5.MockKExtension.Companion.KEEP_MOCKS_PROPERTY
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class MockKExtensionAfterAllTestTest {

    @Test
    open fun prepareAfterAllUnmockTest() {
        mockkObject(TestMock)
        assertTrue(isMockKMock(TestMock, objectMock = true))
    }

    @ExtendWith(CheckIsNotMock::class, MockKExtension::class)
    class AfterAllClearMocks : MockKExtensionAfterAllTestTest()

    @MockKExtension.KeepMocks
    @ExtendWith(CheckIsStillMock::class, MockKExtension::class)
    class AnnotatedClassAfterAllKeepMocks : MockKExtensionAfterAllTestTest()

    @ExtendWith(CheckIsStillMock::class, MockKExtension::class)
    class AnnotatedMethodAfterAllKeepMocks : MockKExtensionAfterAllTestTest() {

        @MockKExtension.KeepMocks
        override fun prepareAfterAllUnmockTest() = super.prepareAfterAllUnmockTest()

    }

    @ExtendWith(CheckIsStillMock::class, PropertyExtension::class, MockKExtension::class)
    class PropertyAfterAllKeepMocks : MockKExtensionAfterAllTestTest()

    object TestMock

    class CheckIsNotMock : AfterAllCallback {

        override fun afterAll(context: ExtensionContext) {
            assertFalse(isMockKMock(TestMock, objectMock = true))
        }

    }

    class CheckIsStillMock : AfterAllCallback {

        override fun afterAll(context: ExtensionContext) {
            assertTrue(isMockKMock(TestMock, objectMock = true))
            unmockkObject(TestMock)
        }

    }

    class PropertyExtension : BeforeAllCallback, AfterAllCallback {

        override fun beforeAll(context: ExtensionContext?) {
            System.setProperty(KEEP_MOCKS_PROPERTY, "true")
        }

        override fun afterAll(context: ExtensionContext) {
            System.clearProperty(KEEP_MOCKS_PROPERTY)
        }

    }

}