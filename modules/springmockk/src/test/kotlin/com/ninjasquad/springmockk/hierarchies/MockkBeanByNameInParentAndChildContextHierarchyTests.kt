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
import com.ninjasquad.springmockk.example.RealExampleService
import com.ninjasquad.springmockk.hierarchies.MockkBeanByNameInParentAndChildContextHierarchyTests.Config1
import com.ninjasquad.springmockk.hierarchies.MockkBeanByNameInParentAndChildContextHierarchyTests.Config2
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.ContextHierarchy
import org.springframework.test.context.aot.DisabledInAotMode
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Verifies that [@MockkBean][MockkBean] can be used within a
 * [@ContextHierarchy][ContextHierarchy] with named context levels, when
 * identical beans are mocked "by name" in the parent and in the child.
 *
 * @author Sam Brannen
 * @since 6.2.6
 */
@ExtendWith(SpringExtension::class)
@ContextHierarchy(
    ContextConfiguration(
        classes = [Config1::class],
        name = "parent"
    ),
    ContextConfiguration(
        classes = [Config2::class],
        name = "child"
    )
)
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class MockkBeanByNameInParentAndChildContextHierarchyTests {
    @MockkBean(name = "service", contextName = "parent")
    lateinit var serviceInParent: ExampleService

    @MockkBean(name = "service", contextName = "child")
    lateinit var serviceInChild: ExampleService

    @Autowired
    lateinit var serviceCaller1: ExampleServiceCaller

    @Autowired
    lateinit var serviceCaller2: ExampleServiceCaller


    @Test
    fun test() {
        every { serviceInParent.greeting() } returns "Mock 1"
        every { serviceInChild.greeting() } returns "Mock 2"

        assertThat(serviceInParent.greeting()).isEqualTo("Mock 1")
        assertThat(serviceInChild.greeting()).isEqualTo("Mock 2")
        assertThat(serviceCaller1.service).isSameAs(serviceInParent)
        assertThat(serviceCaller2.service).isSameAs(serviceInChild)
        assertThat(serviceCaller1.sayGreeting()).isEqualTo("I say Mock 1")
        assertThat(serviceCaller2.sayGreeting()).isEqualTo("I say Mock 2")
    }


    @Configuration
    class Config1 {
        @Bean
        fun service(): ExampleService {
            return RealExampleService("Service 1")
        }

        @Bean
        fun serviceCaller1(service: ExampleService): ExampleServiceCaller {
            return ExampleServiceCaller(service)
        }
    }

    @Configuration
    class Config2 {
        @Bean
        fun service(): ExampleService {
            return RealExampleService("Service 2")
        }

        @Bean
        fun serviceCaller2(service: ExampleService): ExampleServiceCaller {
            return ExampleServiceCaller(service)
        }
    }
}
