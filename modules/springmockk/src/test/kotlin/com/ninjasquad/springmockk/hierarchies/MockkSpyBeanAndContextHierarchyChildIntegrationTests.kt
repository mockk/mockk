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

import com.ninjasquad.springmockk.MockkSpyBean
import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.ExampleServiceCaller
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.ContextHierarchy
import org.springframework.test.context.aot.DisabledInAotMode

/**
 * Tests which verify that [@MockkBean][MockkBean] and
 * [@MockkSpyBean][MockkSpyBean] can be used within a
 * [@ContextHierarchy][ContextHierarchy].
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 * @see MockkBeanAndContextHierarchyParentIntegrationTests
 */
@ContextHierarchy(ContextConfiguration())
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class MockkSpyBeanAndContextHierarchyChildIntegrationTests : MockkBeanAndContextHierarchyParentIntegrationTests() {
    @MockkSpyBean
    lateinit var serviceCaller: ExampleServiceCaller


    @Test
    override fun test(context: ApplicationContext) {
        val parentContext = context.getParent()!!
        assertThat(parentContext).`as`("parent ApplicationContext").isNotNull()
        assertThat(parentContext.getParent()).`as`("grandparent ApplicationContext")
            .isNull()

        assertThat(parentContext.getBeanNamesForType(ExampleService::class.java)).hasSize(1)
        assertThat(parentContext.getBeanNamesForType(ExampleServiceCaller::class.java)).isEmpty()

        assertThat(context.getBeanNamesForType(ExampleService::class.java)).hasSize(1)
        assertThat(context.getBeanNamesForType(ExampleServiceCaller::class.java)).hasSize(1)

        assertThat(service.greeting()).isEqualTo("mock")
        assertThat(serviceCaller.sayGreeting()).isEqualTo("I say mock")
    }


    @Configuration(proxyBeanMethods = false)
    class ChildConfig {
        @Bean
        fun serviceCaller(service: ExampleService): ExampleServiceCaller {
            return ExampleServiceCaller(service)
        }
    }
}
