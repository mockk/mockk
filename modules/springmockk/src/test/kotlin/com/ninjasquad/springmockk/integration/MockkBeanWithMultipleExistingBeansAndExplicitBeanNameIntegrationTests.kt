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
package com.ninjasquad.springmockk.integration

import com.ninjasquad.springmockk.MockkAssertions.assertIsMock
import com.ninjasquad.springmockk.MockkAssertions.assertMockName
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.example.ExampleGenericServiceCaller
import com.ninjasquad.springmockk.example.IntegerExampleGenericService
import com.ninjasquad.springmockk.example.StringExampleGenericService
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Tests that [@MockkBean][MockkBean] can be used to mock a bean when
 * there are multiple candidates and an explicit bean name is supplied to select
 * one of the candidates.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 * @see MockkBeanWithMultipleExistingBeansAndExplicitQualifierIntegrationTests
 *
 * @see MockkBeanWithMultipleExistingBeansAndOnePrimaryIntegrationTests
 */
@SpringJUnitConfig
class MockkBeanWithMultipleExistingBeansAndExplicitBeanNameIntegrationTests {
    @MockkBean("stringService")
    lateinit var mock: StringExampleGenericService

    @Autowired
    lateinit var caller: ExampleGenericServiceCaller


    @Test
    fun test() {
        assertIsMock(mock)
        assertMockName(mock, "stringService")

        every { mock.greeting() } returns "mocked"
        assertThat(caller.sayGreeting()).isEqualTo("I say mocked 123")
        verify { mock.greeting() }
    }


    @Configuration(proxyBeanMethods = false)
    @Import(ExampleGenericServiceCaller::class, IntegerExampleGenericService::class)
    class Config {
        @Bean
        fun one(): StringExampleGenericService? {
            return StringExampleGenericService("one")
        }

        @Bean
        fun stringService(): StringExampleGenericService? {
            return StringExampleGenericService("two")
        }
    }
}
