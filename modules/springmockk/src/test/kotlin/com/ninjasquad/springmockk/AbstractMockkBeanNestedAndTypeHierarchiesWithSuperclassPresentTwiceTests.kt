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
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Abstract top-level class and abstract inner class for integration tests for
 * [@MockkBean][MockkBean] which verify that `@MockkBean` fields
 * are not discovered more than once when searching intertwined enclosing class
 * hierarchies and type hierarchies, when a superclass is *present* twice
 * in the intertwined hierarchies.
 *
 * @author Sam Brannen
 * @since 6.2.7
 * @see MockkBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests
 *
 * @see [gh-34844](https://github.com/spring-projects/spring-framework/issues/34844)
 */
@ExtendWith(SpringExtension::class)
abstract class AbstractMockkBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests {
    @Autowired
    lateinit var enclosingContext: ApplicationContext

    @MockkBean
    lateinit var service: ExampleService


    @Test
    open fun topLevelTest() {
        assertIsMock(service)
        assertThat(enclosingContext.getBeanNamesForType(ExampleService::class.java)).hasSize(1)
    }


    abstract inner class AbstractBaseClassForNestedTests {
        @Test
        fun nestedTest(nestedContext: ApplicationContext?) {
            assertIsMock(service)
            assertThat(enclosingContext).isSameAs(nestedContext)
            assertThat(enclosingContext.getBeanNamesForType(ExampleService::class.java)).hasSize(1)
        }
    }
}
