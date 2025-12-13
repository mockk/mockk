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

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.test.context.ContextCustomizer
import kotlin.reflect.KClass

/**
 * Tests that validate the behavior of [MockkBean] and
 * [MockkSpyBean] with the TCF context cache.
 *
 * @author Stephane Nicoll
 */
class MockkBeanContextCustomizerEqualityTests {
    @Test
    fun contextCustomizerWithSameMockByNameInDifferentClassIsEqual() {
        assertThat(customizerFor(Case1ByName::class)).isEqualTo(customizerFor(Case2ByName::class))
    }

    @Test
    fun contextCustomizerWithSameMockByTypeInDifferentClassIsEqual() {
        assertThat(customizerFor(Case1ByType::class))
            .isEqualTo(customizerFor(Case2ByTypeSameFieldName::class))
    }

    @Test
    fun contextCustomizerWithSameMockByTypeAndDifferentFieldNamesAreNotEqual() {
        assertThat(customizerFor(Case1ByType::class))
            .isNotEqualTo(customizerFor(Case2ByType::class))
    }

    @Test
    fun contextCustomizerWithSameSpyByNameInDifferentClassIsEqual() {
        assertThat(customizerFor(Case4ByName::class)).isEqualTo(customizerFor(Case5ByName::class))
    }

    @Test
    fun contextCustomizerWithSameSpyByTypeInDifferentClassIsEqual() {
        assertThat(customizerFor(Case4ByType::class))
            .isEqualTo(customizerFor(Case5ByTypeSameFieldName::class))
    }

    @Test
    fun contextCustomizerWithSameSpyByTypeAndDifferentFieldNamesAreNotEqual() {
        assertThat(customizerFor(Case4ByType::class))
            .isNotEqualTo(customizerFor(Case5ByType::class))
    }

    @Test
    fun contextCustomizerWithSimilarMockButDifferentRelaxedIsNotEqual() {
        assertThat(customizerFor(Case1ByType::class)).isNotEqualTo(customizerFor(Case3::class))
    }

    @Test
    fun contextCustomizerWithSimilarMockButDifferentRelaxUnitFunIsNotEqual() {
        assertThat(customizerFor(Case1ByType::class)).isNotEqualTo(customizerFor(Case3RelaxUnitFun::class))
    }

    @Test
    fun contextCustomizerWithMockAndSpyAreNotEqual() {
        assertThat(customizerFor(Case1ByType::class))
            .isNotEqualTo(customizerFor(Case4ByType::class))
    }

    private fun customizerFor(testClass: KClass<*>): ContextCustomizer {
        val customizer= BeanOverrideContextCustomizerTestUtils.createContextCustomizer(testClass)
        assertThat(customizer).isNotNull()
        return customizer!!
    }

    class Case1ByName {
        @MockkBean("serviceBean")
        private lateinit var exampleService: String
    }

    class Case1ByType {
        @MockkBean
        private lateinit var exampleService: String
    }

    class Case2ByName {
        @MockkBean("serviceBean")
        private lateinit var serviceToMock: String
    }

    class Case2ByType {
        @MockkBean
        private lateinit var serviceToMock: String
    }

    class Case2ByTypeSameFieldName {
        @MockkBean
        private lateinit var exampleService: String
    }

    class Case3 {
        @MockkBean(relaxed = true)
        private lateinit var exampleService: String
    }

    class Case3RelaxUnitFun {
        @MockkBean(relaxUnitFun = true)
        private lateinit var exampleService: String
    }

    class Case4ByName {
        @MockkSpyBean("serviceBean")
        private lateinit var exampleService: String
    }

    class Case4ByType {
        @MockkSpyBean
        private lateinit var exampleService: String
    }

    class Case5ByName {
        @MockkSpyBean("serviceBean")
        private lateinit var serviceToMock: String
    }

    class Case5ByType {
        @MockkSpyBean
        private lateinit var serviceToMock: String
    }

    class Case5ByTypeSameFieldName {
        @MockkSpyBean
        private lateinit var exampleService: String
    }
}
