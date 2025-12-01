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

import com.ninjasquad.springmockk.MockkAssertions.assertIsSpy
import com.ninjasquad.springmockk.MockkAssertions.assertMockName
import com.ninjasquad.springmockk.MockkSpyBean
import com.ninjasquad.springmockk.example.ExampleGenericServiceCaller
import com.ninjasquad.springmockk.example.IntegerExampleGenericService
import com.ninjasquad.springmockk.example.StringExampleGenericService
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Tests that [@MockkSpyBean][MockkSpyBean] can be used to spy on a bean
 * when there are multiple candidates and one is [@Primary][Primary].
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see MockkSpyBeanWithMultipleExistingBeansAndExplicitBeanNameIntegrationTests
 *
 * @see MockkSpyBeanWithMultipleExistingBeansAndExplicitQualifierIntegrationTests
 */
@ExtendWith(SpringExtension::class)
class MockkSpyBeanWithMultipleExistingBeansAndOnePrimaryIntegrationTests {
    @MockkSpyBean
    lateinit var spy: StringExampleGenericService

    @Autowired
    lateinit var caller: ExampleGenericServiceCaller


    @Test
    fun testSpying() {
        assertIsSpy(spy)
        assertMockName(spy, "two")

        assertThat(caller.sayGreeting()).isEqualTo("I say two 123")
        verify { spy.greeting() }
    }


    @Configuration(proxyBeanMethods = false)
    @Import(ExampleGenericServiceCaller::class, IntegerExampleGenericService::class)
    class Config {
        @Bean
        fun one(): StringExampleGenericService? {
            return StringExampleGenericService("one")
        }

        @Bean
        @Primary
        fun two(): StringExampleGenericService? {
            return StringExampleGenericService("two")
        }
    }
}
