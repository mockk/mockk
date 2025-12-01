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

import com.ninjasquad.springmockk.MockkAssertions.assertIsMock
import com.ninjasquad.springmockk.example.ExampleService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [@MockkBean][MockkBean] where duplicate mocks
 * are created for the same nonexistent type, selected by-type.
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see [gh-34025](https://github.com/spring-projects/spring-framework/issues/34025)
 *
 * @see MockkBeanDuplicateTypeReplacementIntegrationTests
 *
 * @see MockkSpyBeanDuplicateTypeIntegrationTests
 */
@SpringJUnitConfig
class MockkBeanDuplicateTypeCreationIntegrationTests {
    @MockkBean
    lateinit var mock1: ExampleService

    @MockkBean
    lateinit var mock2: ExampleService

    @Autowired
    lateinit var services: List<ExampleService>


    @Test
    fun duplicateMocksShouldHaveBeenCreated() {
        assertThat(services).containsExactly(mock1, mock2)
        assertThat(mock1).isNotSameAs(mock2)
        assertIsMock(mock1)
        assertIsMock(mock2)
    }
}
