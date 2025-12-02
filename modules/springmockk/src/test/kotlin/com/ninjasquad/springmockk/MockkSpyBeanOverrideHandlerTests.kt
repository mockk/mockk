/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ninjasquad.springmockk

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.util.ReflectionUtils

/**
 * Tests for [MockkSpyBeanOverrideHandler].
 *
 * @author Stephane Nicoll
 */
class MockkSpyBeanOverrideHandlerTests {
    @Test
    fun beanNameIsSetToNullIfAnnotationNameIsEmpty() {
        val list = BeanOverrideTestUtils.findHandlers(SampleOneSpy::class.java)
        assertThat(list).singleElement()
            .satisfies({ handler ->
                assertThat(handler.getBeanName()).isNull()
            })
    }

    @Test
    fun beanNameIsSetToAnnotationName() {
        val list = BeanOverrideTestUtils.findHandlers(SampleOneSpyWithName::class.java)
        assertThat(list).singleElement()
            .satisfies({ handler ->
                assertThat(handler.getBeanName()).isEqualTo("anotherService")
            })
    }

    @Test
    fun isEqualToWithSameInstance() {
        val handler = handlerFor("service")
        assertThat(handler).isEqualTo(handler)
        assertThat(handler).hasSameHashCodeAs(handler)
    }

    @Test
    fun isEqualToWithSameMetadata() {
        val handler1 = handlerFor("service")
        val handler2 = handlerFor("service")
        assertThat(handler1).isEqualTo(handler2)
        assertThat(handler1).hasSameHashCodeAs(handler2)
    }

    @Test
    fun isNotEqualToByTypeLookupWithSameMetadataButDifferentField() {
        assertThat(handlerFor("service"))
            .isNotEqualTo(handlerFor("service2"))
    }

    @Test
    fun isEqualToByNameLookupWithSameMetadataButDifferentField() {
        val handler1 = handlerFor("service3")
        val handler2 = handlerFor("service4")
        assertThat(handler1).isEqualTo(handler2)
        assertThat(handler1).hasSameHashCodeAs(handler2)
    }

    @Test
    fun isNotEqualToWithSameMetadataButDifferentBeanName() {
        assertThat(handlerFor("service"))
            .isNotEqualTo(handlerFor("service3"))
    }

    @Test
    fun isNotEqualToWithSameMetadataButDifferentReset() {
        assertThat(handlerFor("service"))
            .isNotEqualTo(handlerFor("service5"))
    }


    class SampleOneSpy {
        @MockkSpyBean
        lateinit var service: String
    }

    class SampleOneSpyWithName {
        @MockkSpyBean("anotherService")
        lateinit var service: String
    }

    class Sample {
        @MockkSpyBean
        private lateinit var service: String

        @MockkSpyBean
        private lateinit var service2: String

        @MockkSpyBean(name = "beanToSpy")
        private lateinit var service3: String

        @MockkSpyBean(value = "beanToSpy")
        private lateinit var service4: String

        @MockkSpyBean(clear = MockkClear.BEFORE)
        private lateinit var service5: String
    }

    companion object {
        private fun handlerFor(fieldName: String): MockkSpyBeanOverrideHandler {
            val field = ReflectionUtils.findField(Sample::class.java, fieldName)
            assertThat(field).isNotNull()
            val annotation: MockkSpyBean =
                AnnotatedElementUtils.getMergedAnnotation(field!!, MockkSpyBean::class.java)!!
            return MockkSpyBeanOverrideHandler(field, ResolvableType.forClass(field.getType()), annotation)
        }
    }
}
