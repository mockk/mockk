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

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.example.ExampleGenericService
import com.ninjasquad.springmockk.example.ExampleGenericServiceCaller
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Tests that [@MockkBean][MockkBean] on fields with generics can be used
 * to inject new mock instances.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see MockkSpyBeanWithGenericsOnTestFieldForExistingGenericBeanIntegrationTests
 */
@SpringJUnitConfig
class MockkBeanWithGenericsOnTestFieldForNewBeanIntegrationTests {
    @MockkBean
    lateinit var stringService: ExampleGenericService<String>

    @MockkBean
    lateinit var integerService: ExampleGenericService<Int>

    @Autowired
    lateinit var caller: ExampleGenericServiceCaller


    @Test
    fun testMocking() {
        every { stringService.greeting() } returns "Hello"
        every { integerService.greeting() } returns 42
        assertThat(caller.sayGreeting()).isEqualTo("I say Hello 42")
    }


    @Configuration(proxyBeanMethods = false)
    @Import(ExampleGenericServiceCaller::class)
    class Config
}
