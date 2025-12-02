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

import com.ninjasquad.springmockk.MockkSpyBean
import com.ninjasquad.springmockk.example.ExampleGenericService
import com.ninjasquad.springmockk.example.ExampleGenericServiceCaller
import com.ninjasquad.springmockk.example.IntegerExampleGenericService
import com.ninjasquad.springmockk.example.StringExampleGenericService
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Tests that [@MockkSpyBean][MockkSpyBean] on a field with generics can
 * be used to replace an existing bean with matching generics.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see MockkBeanWithGenericsOnTestFieldForNewBeanIntegrationTests
 *
 * @see MockkSpyBeanWithGenericsOnTestFieldForExistingGenericBeanProducedByFactoryBeanIntegrationTests
 */
@SpringJUnitConfig
class MockkSpyBeanWithGenericsOnTestFieldForExistingGenericBeanIntegrationTests {
    @MockkSpyBean
    lateinit var service: ExampleGenericService<String>

    @Autowired
    lateinit var caller: ExampleGenericServiceCaller


    @Test
    fun testSpying() {
        assertThat(caller.sayGreeting()).isEqualTo("I say Enigma 123")
        verify { service.greeting() }
    }


    @Configuration(proxyBeanMethods = false)
    @Import(ExampleGenericServiceCaller::class, IntegerExampleGenericService::class)
    class Config {
        @Bean
        fun simpleExampleStringGenericService(): ExampleGenericService<String> {
            // In order to trigger the issue, we need a method signature that returns the
            // generic type instead of the actual implementation class.
            return StringExampleGenericService("Enigma")
        }
    }
}
