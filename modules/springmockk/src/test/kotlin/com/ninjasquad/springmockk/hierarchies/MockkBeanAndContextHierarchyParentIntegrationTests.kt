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
package com.ninjasquad.springmockk.hierarchies

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.ExampleServiceCaller
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Tests which verify that [@MockkBean][MockkBean] can be used within a
 * [@ContextHierarchy][ContextHierarchy].
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 * @see MockkSpyBeanAndContextHierarchyChildIntegrationTests
 */
@SpringJUnitConfig
open class MockkBeanAndContextHierarchyParentIntegrationTests {
    @MockkBean
    lateinit var service: ExampleService


    @BeforeEach
    fun configureServiceMock() {
        every { service.greeting() } returns "mock"
    }

    @Test
    open fun test(context: ApplicationContext) {
        assertThat(context.getBeanNamesForType(ExampleService::class.java)).hasSize(1)
        assertThat(context.getBeanNamesForType(ExampleServiceCaller::class.java)).isEmpty()

        assertThat(service.greeting()).isEqualTo("mock")
    }
}
