package com.ninjasquad.springmockk

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.test.context.bean.override.BeanOverrideHandler
import org.springframework.util.ReflectionUtils
import java.io.Externalizable
import java.lang.reflect.Field

/**
 * Tests for [MockkBeanOverrideHandler].
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
class MockkBeanOverrideHandlerTests {
    @Test
    fun beanNameIsSetToNullIfAnnotationNameIsEmpty() {
        val list = BeanOverrideTestUtils.findHandlers(SampleOneMock::class.java)
        assertThat(list).singleElement()
            .satisfies(ThrowingConsumer { handler: BeanOverrideHandler? ->
                assertThat(handler!!.getBeanName()).isNull()
            })
    }

    @Test
    fun beanNameIsSetToAnnotationName() {
        val list = BeanOverrideTestUtils.findHandlers(SampleOneMockWithName::class.java)
        assertThat(list).singleElement()
            .satisfies(ThrowingConsumer { handler: BeanOverrideHandler? ->
                assertThat(handler!!.getBeanName()).isEqualTo("anotherService")
            })
    }

    @Test
    fun isEqualToWithSameInstanceFromField() {
        val handler: MockkBeanOverrideHandler = createHandler(sampleField("service"))
        assertThat(handler).isEqualTo(handler)
        assertThat(handler).hasSameHashCodeAs(handler)
    }

    @Test
    fun isEqualToWithSameMetadataFromField() {
        val handler1: MockkBeanOverrideHandler = createHandler(sampleField("service"))
        val handler2: MockkBeanOverrideHandler = createHandler(sampleField("service"))
        assertThat(handler1).isEqualTo(handler2)
        assertThat(handler1).hasSameHashCodeAs(handler2)
    }

    @Test
    fun isEqualToWithSameInstanceFromClassLevel() {
        val handler1: MockkBeanOverrideHandler = createHandler(ClassLevelStringMockByName1::class.java)
        assertThat(handler1).isEqualTo(handler1)
        assertThat(handler1).hasSameHashCodeAs(handler1)

        val handler2: MockkBeanOverrideHandler = createHandler(ClassLevelStringMockByType1::class.java)
        assertThat(handler2).isEqualTo(handler2)
        assertThat(handler2).hasSameHashCodeAs(handler2)
    }

    @Test
    fun isEqualToWithSameByNameLookupMetadataFromClassLevel() {
        val handler1: MockkBeanOverrideHandler = createHandler(ClassLevelStringMockByName1::class.java)
        val handler2: MockkBeanOverrideHandler = createHandler(ClassLevelStringMockByName2::class.java)
        assertThat(handler1).isEqualTo(handler2)
        assertThat(handler2).isEqualTo(handler1)
        assertThat(handler1).hasSameHashCodeAs(handler2)
    }

    @Test
    fun isNotEqualToWithDifferentByNameLookupMetadataFromClassLevel() {
        val handler1: MockkBeanOverrideHandler = createHandler(ClassLevelStringMockByName1::class.java)
        val handler2: MockkBeanOverrideHandler = createHandler(ClassLevelStringMockByName3::class.java)
        assertThat(handler1).isNotEqualTo(handler2)
        assertThat(handler2).isNotEqualTo(handler1)
        assertThat(handler1).doesNotHaveSameHashCodeAs(handler2)
    }

    @Test
    fun isEqualToWithSameByTypeLookupMetadataFromClassLevel() {
        val handler1: MockkBeanOverrideHandler = createHandler(ClassLevelStringMockByType1::class.java)
        val handler2: MockkBeanOverrideHandler = createHandler(ClassLevelStringMockByType2::class.java)
        assertThat(handler1).isEqualTo(handler2)
        assertThat(handler2).isEqualTo(handler1)
        assertThat(handler1).hasSameHashCodeAs(handler2)
    }

    @Test
    fun isNotEqualToWithDifferentByTypeLookupMetadataFromClassLevel() {
        val handler1: MockkBeanOverrideHandler = createHandler(ClassLevelStringMockByType1::class.java)
        val handler2: MockkBeanOverrideHandler = createHandler(ClassLevelStringMockByType3::class.java)
        assertThat(handler1).isNotEqualTo(handler2)
        assertThat(handler2).isNotEqualTo(handler1)
        assertThat(handler1).doesNotHaveSameHashCodeAs(handler2)
    }

    @Test
    fun isEqualToWithSameByNameLookupMetadataFromFieldAndClassLevel() {
        val handler1: MockkBeanOverrideHandler = createHandler(sampleField("service3"))
        val handler2: MockkBeanOverrideHandler = createHandler(ClassLevelStringMockByName1::class.java)
        assertThat(handler1).isEqualTo(handler2)
        assertThat(handler2).isEqualTo(handler1)
        assertThat(handler1).hasSameHashCodeAs(handler2)
    }

    /**
     * Since the "field name as fallback qualifier" is not available for an annotated class,
     * what would seem to be "equivalent" handlers are actually not considered "equal" when
     * the lookup is "by type".
     */
    @Test
    fun isNotEqualToWithSameByTypeLookupMetadataFromFieldAndClassLevel() {
        val handler1: MockkBeanOverrideHandler = createHandler(sampleField("service"))
        val handler2: MockkBeanOverrideHandler = createHandler(ClassLevelStringMockByType1::class.java)
        assertThat(handler1).isNotEqualTo(handler2)
        assertThat(handler2).isNotEqualTo(handler1)
        assertThat(handler1).doesNotHaveSameHashCodeAs(handler2)
    }

    @Test
    fun isNotEqualEqualToByTypeLookupWithSameMetadataButDifferentField() {
        val handler1: MockkBeanOverrideHandler = createHandler(sampleField("service"))
        val handler2: MockkBeanOverrideHandler = createHandler(sampleField("service2"))
        assertThat(handler1).isNotEqualTo(handler2)
    }

    @Test
    fun isEqualEqualToByNameLookupWithSameMetadataButDifferentField() {
        val handler1: MockkBeanOverrideHandler = createHandler(sampleField("service3"))
        val handler2: MockkBeanOverrideHandler = createHandler(sampleField("service4"))
        assertThat(handler1).isEqualTo(handler2)
        assertThat(handler1).hasSameHashCodeAs(handler2)
    }

    @Test
    fun isNotEqualToWithSameMetadataButDifferentBeanName() {
        val handler1: MockkBeanOverrideHandler = createHandler(sampleField("service"))
        val handler2: MockkBeanOverrideHandler = createHandler(sampleField("service3"))
        assertThat(handler1).isNotEqualTo(handler2)
    }

    @Test
    fun isNotEqualToWithSameMetadataButDifferentExtraInterfaces() {
        val handler1: MockkBeanOverrideHandler = createHandler(sampleField("service"))
        val handler2: MockkBeanOverrideHandler = createHandler(sampleField("service5"))
        assertThat(handler1).isNotEqualTo(handler2)
    }

    @Test
    fun isNotEqualToWithSameMetadataButDifferentRelaxed() {
        val handler1: MockkBeanOverrideHandler = createHandler(sampleField("service"))
        val handler2: MockkBeanOverrideHandler = createHandler(sampleField("service6"))
        assertThat(handler1).isNotEqualTo(handler2)
    }

    @Test
    fun isNotEqualToWithSameMetadataButDifferentRelaxUnitFun() {
        val handler1: MockkBeanOverrideHandler = createHandler(sampleField("service"))
        val handler2: MockkBeanOverrideHandler = createHandler(sampleField("service7"))
        assertThat(handler1).isNotEqualTo(handler2)
    }

    private fun createHandler(clazz: Class<*>): MockkBeanOverrideHandler {
        val annotation = AnnotatedElementUtils.getMergedAnnotation<MockkBean>(clazz, MockkBean::class.java)
        return MockkBeanOverrideHandler(null, ResolvableType.forClass(annotation!!.types[0].java), annotation)
    }

    class SampleOneMock {
        @MockkBean
        var service: String? = null
    }

    class SampleOneMockWithName {
        @MockkBean("anotherService")
        var service: String? = null
    }

    class Sample {
        @MockkBean
        private val service: String? = null

        @MockkBean
        private val service2: String? = null

        @MockkBean(name = "beanToMock")
        private val service3: String? = null

        @MockkBean(value = "beanToMock")
        private val service4: String? = null

        @MockkBean(extraInterfaces = [Externalizable::class])
        private val service5: String? = null

        @MockkBean(relaxed = true)
        private val service6: String? = null

        @MockkBean(relaxUnitFun = true)
        private val service7: String? = null
    }

    @MockkBean(name = "beanToMock", types = [String::class])
    class ClassLevelStringMockByName1

    @MockkBean(name = "beanToMock", types = [String::class])
    class ClassLevelStringMockByName2

    @MockkBean(name = "otherBeanToMock", types = [String::class])
    class ClassLevelStringMockByName3

    @MockkBean(types = [String::class])
    class ClassLevelStringMockByType1

    @MockkBean(types = [String::class])
    class ClassLevelStringMockByType2

    @MockkBean(types = [Int::class])
    class ClassLevelStringMockByType3

    companion object {
        private fun sampleField(fieldName: String): Field {
            val field = ReflectionUtils.findField(Sample::class.java, fieldName)
            assertThat(field).isNotNull()
            return field!!
        }

        private fun createHandler(field: Field): MockkBeanOverrideHandler {
            val annotation = AnnotatedElementUtils.getMergedAnnotation<MockkBean>(field, MockkBean::class.java)
            return MockkBeanOverrideHandler(field, ResolvableType.forClass(field.getType()), annotation!!)
        }
    }
}
