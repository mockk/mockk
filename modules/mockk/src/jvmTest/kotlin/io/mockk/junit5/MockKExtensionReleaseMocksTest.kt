package io.mockk.junit5

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension.Companion.RELEASE_MOCKS_PROPERTY
import io.mockk.mockk
import io.mockk.util.assertGarbageCollected
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.ref.WeakReference

abstract class MockKExtensionReleaseMocksTest {
    @Test
    open fun stubAndCallMocks() {
        repeat(MOCK_COUNT) {
            val mock = mockk<Dependency>()
            every { mock.value() } returns "x"
            mock.value()
            mocks += mock
        }
    }

    @MockKExtension.ReleaseMocks
    @ExtendWith(AssertMocksCollected::class, MockKExtension::class)
    class AnnotatedClassReleasesMocks : MockKExtensionReleaseMocksTest()

    @MockKExtension.ReleaseMocks
    @ExtendWith(AssertMocksCollected::class, MockKExtension::class)
    class AnnotatedClassReleasesInjectedMocks : MockKExtensionReleaseMocksTest() {
        @MockK
        private lateinit var dependency: Dependency

        @Test
        fun stubAndCallInjectedMock() {
            every { dependency.value() } returns "x"
            dependency.value()
            mocks += dependency
        }
    }

    @MockKExtension.ReleaseMocks
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
    @ExtendWith(AssertMocksCollected::class, MockKExtension::class)
    class PerClassLifecycleReleasesMocksAfterClass : MockKExtensionReleaseMocksTest() {
        @Order(1)
        @Test
        override fun stubAndCallMocks() = super.stubAndCallMocks()

        @Order(2)
        @Test
        fun mocksStayUsableUntilTheClassFinishes() {
            mocks.forEach { every { it.value() } returns "y" }
        }
    }

    @ExtendWith(AssertMocksCollected::class, PropertyExtension::class, MockKExtension::class)
    class PropertyReleasesMocks : MockKExtensionReleaseMocksTest()

    @ExtendWith(AssertMocksStillUsable::class, MockKExtension::class)
    class DefaultKeepsMocksUsable : MockKExtensionReleaseMocksTest()

    @MockKExtension.ReleaseMocks
    @MockKExtension.KeepMocks
    @ExtendWith(AssertMocksStillUsable::class, MockKExtension::class)
    class KeepMocksWinsOverReleaseMocks : MockKExtensionReleaseMocksTest()

    interface Dependency {
        fun value(): String
    }

    class AssertMocksCollected : AfterAllCallback {
        override fun afterAll(context: ExtensionContext) {
            val refs = takeMocksAsWeakReferences()
            assertGarbageCollected(refs, "mocks still reachable after the extension finished")
        }
    }

    class AssertMocksStillUsable : AfterAllCallback {
        override fun afterAll(context: ExtensionContext) {
            takeMocks().forEach { every { it.value() } returns "y" }
        }
    }

    class PropertyExtension :
        BeforeAllCallback,
        AfterAllCallback {
        override fun beforeAll(context: ExtensionContext) {
            System.setProperty(RELEASE_MOCKS_PROPERTY, "true")
        }

        override fun afterAll(context: ExtensionContext) {
            System.clearProperty(RELEASE_MOCKS_PROPERTY)
        }
    }

    companion object {
        private const val MOCK_COUNT = 100
        private val mocks = mutableListOf<Dependency>()

        private fun takeMocks(): List<Dependency> {
            val taken = mocks.toList()
            mocks.clear()
            return taken
        }

        private fun takeMocksAsWeakReferences(): List<WeakReference<Dependency>> {
            val refs = mocks.map { WeakReference(it) }
            mocks.clear()
            return refs
        }
    }
}
