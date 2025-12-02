package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.ExampleServiceCaller
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType
import java.util.*
import java.util.Collections.emptySet


/**
 * Tests for [MockkContextCustomizer].
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class MockkContextCustomizerTests {

    @Test
    fun hashCodeAndEquals() {
        val d1 = createTestMockDefinition(ExampleService::class.java)
        val d2 = createTestMockDefinition(ExampleServiceCaller::class.java)
        val c1 = MockkContextCustomizer(emptySet())
        val c2 = MockkContextCustomizer(LinkedHashSet(listOf(d1, d2)))
        val c3 = MockkContextCustomizer(LinkedHashSet(listOf(d2, d1)))
        assertThat(c2.hashCode()).isEqualTo(c3.hashCode())
        assertThat(c1).isEqualTo(c1).isNotEqualTo(c2)
        assertThat(c2).isEqualTo(c2).isEqualTo(c3).isNotEqualTo(c1)
    }

    private fun createTestMockDefinition(typeToMock: Class<*>): MockkDefinition {
        return MockkDefinition(typeToMock = ResolvableType.forClass(typeToMock))
    }

}
