package io.mockk.springmockk

import io.mockk.mockk
import io.mockk.springmockk.example.ExampleService
import io.mockk.springmockk.example.RealExampleService
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test


/**
 * Tests for [MockkClear].
 *
 * @author JB Nizet
 */
class MockkClearTests {

    @Test
    fun `a simple mock should have NONE as clear` () {
        val mock = mockk<ExampleService>()
        assertThat(MockkClear.get(mock)).isEqualTo(MockkClear.NONE)
    }

    @Test
    fun `a mock cleared with BEFORE has a BEFORE clear`() {
        val mock = mockk<ExampleService>().clear(MockkClear.NONE)
        assertThat(MockkClear.get(mock)).isEqualTo(MockkClear.NONE)
    }

    @Test
    fun `a spy cleared with BEFORE has a BEFORE clear`() {
        val spy = spyk(RealExampleService("hello")).clear(MockkClear.NONE)
        assertThat(MockkClear.get(spy)).isEqualTo(MockkClear.NONE)
    }

    @Test
    fun `only mocks can be cleared`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            RealExampleService("hello").clear(MockkClear.NONE)
        }
    }
}
