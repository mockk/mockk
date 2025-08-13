package io.mockk.it

import io.mockk.impl.annotations.AdditionalInterface
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertNotNull

/**
 * Test related to GitHub issue #445
 */
class MockKExtensionOnNestedClassTest {
    open class Foo

    @Nested
    @ExtendWith(MockKExtension::class)
    inner class InjectMockInNestedTest(
        @MockK private val foo: Foo,
        @MockK @AdditionalInterface(Runnable::class) private val bar: Foo) {

        @Test
        fun shouldHaveInjectMock() {
            assertNotNull(foo, message = "Should have been injected")
        }
    }

}
