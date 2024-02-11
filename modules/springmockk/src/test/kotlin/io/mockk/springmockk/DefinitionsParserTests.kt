package io.mockk.springmockk

import io.mockk.springmockk.example.ExampleExtraInterface
import io.mockk.springmockk.example.ExampleService
import io.mockk.springmockk.example.ExampleServiceCaller
import io.mockk.springmockk.example.RealExampleService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.util.ReflectionUtils

/**
 * Tests for [DefinitionsParser].
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class DefinitionsParserTests {

    private val parser = DefinitionsParser()

    private val definitions: List<Definition>
        get() = this.parser.parsedDefinitions.toList()

    @Test
    fun parseSingleMockBean() {
        this.parser.parse(SingleMockBean::class.java)
        assertThat(definitions).hasSize(1)
        val definition = getMockDefinition(0)
        assertThat(definition.typeToMock.resolve()).isEqualTo(ExampleService::class.java)
        assertThat(definition.name).isNull()
    }

    @Test
    fun parseRepeatMockBean() {
        this.parser.parse(RepeatMockBean::class.java)
        assertThat(definitions).hasSize(2)
        assertThat(getMockDefinition(0).typeToMock.resolve()).isEqualTo(ExampleService::class.java)
        assertThat(getMockDefinition(1).typeToMock.resolve()).isEqualTo(ExampleServiceCaller::class.java)
    }

    @Test
    fun parseMockBeanAttributes() {
        this.parser.parse(MockBeanAttributes::class.java)
        assertThat(definitions).hasSize(1)
        val definition = getMockDefinition(0)
        assertThat(definition.name).isEqualTo("Name")
        assertThat(definition.typeToMock.resolve()).isEqualTo(ExampleService::class.java)
        assertThat(definition.extraInterfaces).containsExactly(ExampleExtraInterface::class)
        assertThat(definition.relaxed).isTrue()
        assertThat(definition.relaxUnitFun).isTrue()
        assertThat(definition.clear).isEqualTo(MockkClear.NONE)
        assertThat(definition.qualifier).isNull()
    }

    @Test
    fun parseMockBeanOnClassAndField() {
        this.parser.parse(MockBeanOnClassAndField::class.java)
        assertThat(definitions).hasSize(2)
        val classDefinition = getMockDefinition(0)
        assertThat(classDefinition.typeToMock.resolve()).isEqualTo(ExampleService::class.java)
        assertThat(classDefinition.qualifier).isNull()
        val fieldDefinition = getMockDefinition(1)
        assertThat(fieldDefinition.typeToMock.resolve()).isEqualTo(ExampleServiceCaller::class.java)
        val qualifier = QualifierDefinition.forElement(
            ReflectionUtils.findField(MockBeanOnClassAndField::class.java, "caller")!!
        )
        assertThat(fieldDefinition.qualifier).isNotNull().isEqualTo(qualifier)
    }

    @Test
    fun parseMockBeanInferClassToMock() {
        this.parser.parse(MockBeanInferClassToMock::class.java)
        assertThat(definitions).hasSize(1)
        assertThat(getMockDefinition(0).typeToMock.resolve()).isEqualTo(ExampleService::class.java)
    }

    @Test
    fun parseMockBeanMissingClassToMock() {
        assertThatIllegalStateException()
            .isThrownBy { this.parser.parse(MockBeanMissingClassToMock::class.java) }
            .withMessageContaining("Unable to deduce type to mock")
    }

    @Test
    fun parseMockBeanMultipleClasses() {
        this.parser.parse(MockBeanMultipleClasses::class.java)
        assertThat(definitions).hasSize(2)
        assertThat(getMockDefinition(0).typeToMock.resolve()).isEqualTo(ExampleService::class.java)
        assertThat(getMockDefinition(1).typeToMock.resolve()).isEqualTo(ExampleServiceCaller::class.java)
    }

    @Test
    fun parseMockBeanMultipleClassesWithName() {
        assertThatIllegalStateException()
            .isThrownBy { this.parser.parse(MockBeanMultipleClassesWithName::class.java) }
            .withMessageContaining(
                "The name attribute can only be used when mocking a single class"
            )
    }

    @Test
    fun parseSingleSpyBean() {
        this.parser.parse(SingleSpyBean::class.java)
        assertThat(definitions).hasSize(1)
        val definition = getSpyDefinition(0)
        assertThat(definition.typeToSpy.resolve()).isEqualTo(RealExampleService::class.java)
        assertThat(definition.name).isNull()
    }

    @Test
    fun parseRepeatSpyBean() {
        this.parser.parse(RepeatSpyBean::class.java)
        assertThat(definitions).hasSize(2)
        assertThat(getSpyDefinition(0).typeToSpy.resolve()).isEqualTo(RealExampleService::class.java)
        assertThat(getSpyDefinition(1).typeToSpy.resolve()).isEqualTo(ExampleServiceCaller::class.java)
    }

    @Test
    fun parseSpyBeanAttributes() {
        this.parser.parse(SpyBeanAttributes::class.java)
        assertThat(definitions).hasSize(1)
        val definition = getSpyDefinition(0)
        assertThat(definition.name).isEqualTo("Name")
        assertThat(definition.typeToSpy.resolve()).isEqualTo(RealExampleService::class.java)
        assertThat(definition.clear).isEqualTo(MockkClear.NONE)
        assertThat(definition.qualifier).isNull()
    }

    @Test
    fun parseSpyBeanOnClassAndField() {
        this.parser.parse(SpyBeanOnClassAndField::class.java)
        assertThat(definitions).hasSize(2)
        val classDefinition = getSpyDefinition(0)
        assertThat(classDefinition.qualifier).isNull()
        assertThat(classDefinition.typeToSpy.resolve()).isEqualTo(RealExampleService::class.java)
        val fieldDefinition = getSpyDefinition(1)
        val qualifier = QualifierDefinition.forElement(
            ReflectionUtils.findField(SpyBeanOnClassAndField::class.java, "caller")!!
        )
        assertThat(fieldDefinition.qualifier).isNotNull().isEqualTo(qualifier)
        assertThat(fieldDefinition.typeToSpy.resolve()).isEqualTo(ExampleServiceCaller::class.java)
    }

    @Test
    fun parseSpyBeanInferClassToMock() {
        this.parser.parse(SpyBeanInferClassToMock::class.java)
        assertThat(definitions).hasSize(1)
        assertThat(getSpyDefinition(0).typeToSpy.resolve()).isEqualTo(RealExampleService::class.java)
    }

    @Test
    fun parseSpyBeanMissingClassToMock() {
        assertThatIllegalStateException()
            .isThrownBy { this.parser.parse(SpyBeanMissingClassToMock::class.java) }
            .withMessageContaining("Unable to deduce type to spy")
    }

    @Test
    fun parseSpyBeanMultipleClasses() {
        this.parser.parse(SpyBeanMultipleClasses::class.java)
        assertThat(definitions).hasSize(2)
        assertThat(getSpyDefinition(0).typeToSpy.resolve()).isEqualTo(RealExampleService::class.java)
        assertThat(getSpyDefinition(1).typeToSpy.resolve()).isEqualTo(ExampleServiceCaller::class.java)
    }

    @Test
    fun parseSpyBeanMultipleClassesWithName() {
        assertThatIllegalStateException()
            .isThrownBy { this.parser.parse(SpyBeanMultipleClassesWithName::class.java) }
            .withMessageContaining(
                "The name attribute can only be used when spying a single class"
            )
    }

    private fun getMockDefinition(index: Int): MockkDefinition {
        return definitions[index] as MockkDefinition
    }

    private fun getSpyDefinition(index: Int): SpykDefinition {
        return definitions[index] as SpykDefinition
    }

    @MockkBean(ExampleService::class)
    internal class SingleMockBean

    @MockkBeans(MockkBean(ExampleService::class), MockkBean(ExampleServiceCaller::class))
    internal class RepeatMockBean

    @MockkBean(
        name = "Name",
        classes = [ExampleService::class],
        extraInterfaces = [ExampleExtraInterface::class],
        relaxed = true,
        relaxUnitFun = true,
        clear = MockkClear.NONE
    )
    internal class MockBeanAttributes

    @MockkBean(ExampleService::class)
    internal class MockBeanOnClassAndField {

        @MockkBean(ExampleServiceCaller::class)
        @Qualifier("test")
        private val caller: Any? = null

    }

    @MockkBean(ExampleService::class, ExampleServiceCaller::class)
    internal class MockBeanMultipleClasses

    @MockkBean(name = "name", classes = [ExampleService::class, ExampleServiceCaller::class])
    internal class MockBeanMultipleClassesWithName

    internal class MockBeanInferClassToMock {

        @MockkBean
        private val exampleService: ExampleService? = null

    }

    @MockkBean
    internal class MockBeanMissingClassToMock

    @SpykBean(RealExampleService::class)
    internal class SingleSpyBean

    @SpykBeans(SpykBean(RealExampleService::class), SpykBean(ExampleServiceCaller::class))
    internal class RepeatSpyBean

    @SpykBean(name = "Name", classes = [RealExampleService::class], clear = MockkClear.NONE)
    internal class SpyBeanAttributes

    @SpykBean(RealExampleService::class)
    internal class SpyBeanOnClassAndField {

        @SpykBean(ExampleServiceCaller::class)
        @Qualifier("test")
        private val caller: Any? = null

    }

    @SpykBean(RealExampleService::class, ExampleServiceCaller::class)
    internal class SpyBeanMultipleClasses

    @SpykBean(name = "name", classes = arrayOf(RealExampleService::class, ExampleServiceCaller::class))
    internal class SpyBeanMultipleClassesWithName

    internal class SpyBeanInferClassToMock {

        @SpykBean
        private val exampleService: RealExampleService? = null

    }

    @SpykBean
    internal class SpyBeanMissingClassToMock

}
