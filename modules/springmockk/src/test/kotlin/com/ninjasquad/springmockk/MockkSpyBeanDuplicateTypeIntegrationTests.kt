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

import com.ninjasquad.springmockk.MockkAssertions.assertIsSpy
import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.RealExampleService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for duplicate [@MockkSpyBean][MockkSpyBean]
 * declarations for the same target bean, selected by-type.
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see [gh-34056](https://github.com/spring-projects/spring-framework/issues/34056)
 *
 * @see MockkBeanDuplicateTypeCreationIntegrationTests
 *
 * @see MockkSpyBeanDuplicateTypeAndNameIntegrationTests
 */
@SpringJUnitConfig
class MockkSpyBeanDuplicateTypeIntegrationTests {
    @MockkSpyBean
    lateinit var spy1: ExampleService

    @MockkSpyBean
    lateinit var spy2: ExampleService

    @Autowired
    lateinit var services: List<ExampleService>


    @Test
    fun onlyOneSpyShouldHaveBeenCreated() {
        // Currently logs something similar to the following.
        //
        // WARN - Bean with name 'exampleService' was overridden by multiple handlers:
        // [MockkSpyBeanOverrideHandler@1d269ed7 ..., MockkSpyBeanOverrideHandler@437ebf59 ...]

        assertThat(services).containsExactly(spy2)
        assertThat(spy1).isSameAs(spy2)

        assertIsSpy(spy2)
    }


    @Configuration(proxyBeanMethods = false)
    class Config {
        @Bean
        fun exampleService(): ExampleService {
            return RealExampleService("@Bean")
        }
    }
}
