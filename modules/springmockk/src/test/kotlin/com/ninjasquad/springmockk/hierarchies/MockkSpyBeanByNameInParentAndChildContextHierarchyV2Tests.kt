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

import com.ninjasquad.springmockk.MockkAssertions.assertIsSpy
import com.ninjasquad.springmockk.MockkSpyBean
import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.ExampleServiceCaller
import com.ninjasquad.springmockk.hierarchies.MockkSpyBeanByNameInParentAndChildContextHierarchyTests.Config1
import com.ninjasquad.springmockk.hierarchies.MockkSpyBeanByNameInParentAndChildContextHierarchyTests.Config2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.ContextHierarchy
import org.springframework.test.context.aot.DisabledInAotMode
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * This is effectively a one-to-one copy of
 * [MockkSpyBeanByNameInParentAndChildContextHierarchyTests], except
 * that this test class uses different names for the context hierarchy levels:
 * level-1 and level-2 instead of parent and child.
 *
 *
 * If the context cache is broken, either this test class or
 * `MockkSpyBeanByNameInParentAndChildContextHierarchyTests` will fail
 * when run within the same test suite.
 *
 * @author Sam Brannen
 * @since 6.2.6
 * @see MockkSpyBeanByNameInParentAndChildContextHierarchyTests
 */
@ExtendWith(SpringExtension::class)
@ContextHierarchy(
    ContextConfiguration(
        classes = [Config1::class],
        name = "level-1"
    ),
    ContextConfiguration(
        classes = [Config2::class],
        name = "level-2"
    )
)
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class MockkSpyBeanByNameInParentAndChildContextHierarchyV2Tests {
    @MockkSpyBean(name = "service", contextName = "level-1")
    lateinit var serviceInParent: ExampleService

    @MockkSpyBean(name = "service", contextName = "level-2")
    lateinit var serviceInChild: ExampleService

    @Autowired
    lateinit var serviceCaller1: ExampleServiceCaller

    @Autowired
    lateinit var serviceCaller2: ExampleServiceCaller


    @Test
    fun test() {
        assertIsSpy(serviceInParent)
        assertIsSpy(serviceInChild)

        assertThat(serviceInParent.greeting()).isEqualTo("Service 1")
        assertThat(serviceInChild.greeting()).isEqualTo("Service 2")
        assertThat(serviceCaller1.service).isSameAs(serviceInParent)
        assertThat(serviceCaller2.service).isSameAs(serviceInChild)
        assertThat(serviceCaller1.sayGreeting()).isEqualTo("I say Service 1")
        assertThat(serviceCaller2.sayGreeting()).isEqualTo("I say Service 2")
    }
}
