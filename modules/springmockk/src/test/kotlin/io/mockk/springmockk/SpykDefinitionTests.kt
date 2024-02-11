package io.mockk.springmockk

import io.mockk.mockk
import io.mockk.springmockk.example.ExampleService
import io.mockk.springmockk.example.ExampleServiceCaller
import io.mockk.springmockk.example.RealExampleService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType


/**
 * Tests for [SpykDefinition].
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class SpykDefinitionTests {

    @Test
    fun createWithDefaults() {
        val definition = SpykDefinition(typeToSpy = REAL_SERVICE_TYPE)
        assertThat(definition.name).isNull()
        assertThat(definition.typeToSpy).isEqualTo(REAL_SERVICE_TYPE)
        assertThat(definition.clear).isEqualTo(MockkClear.AFTER)
        assertThat(definition.qualifier).isNull()
    }

    @Test
    fun createExplicit() {
        val qualifier = mockk<QualifierDefinition>()
        val definition = SpykDefinition(
            name = "name",
            typeToSpy = REAL_SERVICE_TYPE,
            clear = MockkClear.BEFORE,
            qualifier = qualifier
        )
        assertThat(definition.name).isEqualTo("name")
        assertThat(definition.typeToSpy).isEqualTo(REAL_SERVICE_TYPE)
        assertThat(definition.clear).isEqualTo(MockkClear.BEFORE)
        assertThat(definition.qualifier).isEqualTo(qualifier)
    }

    @Test
    fun createSpy() {
        val definition = SpykDefinition(
            name = "name",
            typeToSpy = REAL_SERVICE_TYPE,
            clear = MockkClear.BEFORE
        )
        val spy = definition.createSpy(RealExampleService("hello"))
        assertThat(spy).isInstanceOf(ExampleService::class.java)
        assertThat(spy.toString()).contains("name")
        assertThat(MockkClear.get(spy)).isEqualTo(MockkClear.BEFORE)
    }

    @Test
    fun createSpyWhenWrongInstanceShouldThrowException() {
        val definition = SpykDefinition(
            name = "name",
            typeToSpy = REAL_SERVICE_TYPE,
            clear = MockkClear.BEFORE
        )
        assertThatIllegalArgumentException()
            .isThrownBy { definition.createSpy(ExampleServiceCaller(RealExampleService("hello"))) }
            .withMessageContaining("must be an instance of")
    }

    @Test
    fun createSpyTwice() {
        val definition = SpykDefinition(
            name = "name",
            typeToSpy = REAL_SERVICE_TYPE,
            clear = MockkClear.BEFORE
        )
        var instance: Any = RealExampleService("hello")
        instance = definition.createSpy(instance)
        definition.createSpy(instance)
    }

    companion object {

        private val REAL_SERVICE_TYPE = ResolvableType.forClass(RealExampleService::class.java)
    }

}
