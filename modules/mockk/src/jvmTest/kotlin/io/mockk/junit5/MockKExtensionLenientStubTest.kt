package io.mockk.junit5

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.lenient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

/**
 * With [MockKExtension.CheckUnnecessaryStub] the extension runs `checkUnnecessaryStub()` after
 * every test. These tests pass only if lenient stubs declared with `lenient { ... }` are
 * exempt from that check: had the leniency been ignored, the unused stub of the first test
 * would fail the whole class after the test method completed.
 *
 * The check inspects the stubs of all live mocks, so [BeforeEach] clears stubs left over by
 * previous test classes that don't use the extension (they share the same test JVM).
 */
@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
class MockKExtensionLenientStubTest {
    class MockCls {
        fun op(a: Int) = a + 1
    }

    @MockK
    lateinit var mock: MockCls

    @BeforeEach
    fun clearStubsLeftByOtherTestClasses() {
        clearAllMocks()
    }

    @Test
    fun unusedLenientStubIsNotReportedByTheExtension() {
        lenient {
            every { mock.op(1) } returns 1
        }
        // The stub is never called: the extension check must skip it.
    }

    @Test
    fun usedStubTogetherWithUnusedLenientStubPassesTheExtensionCheck() {
        every { mock.op(2) } returns 2
        lenient {
            every { mock.op(3) } returns 3
        }

        assertEquals(2, mock.op(2))
        // The lenient stub of op(3) is never called: the extension check must skip it.
    }
}
