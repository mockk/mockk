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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Integration tests for [@MockkBean][MockkBean] which verify that
 * `@MockkBean` fields are not discovered more than once when searching
 * intertwined enclosing class hierarchies and type hierarchies, when an enclosing
 * class is *present* twice in the intertwined hierarchies.
 *
 * @author Sam Brannen
 * @since 6.2.3
 * @see MockkBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests
 * @see MockkBeanWithInterfacePresentTwiceTests
 *
 * @see [gh-34324](https://github.com/spring-projects/spring-framework/issues/34324)
 */
@ExtendWith(SpringExtension::class)
class MockkBeanNestedAndTypeHierarchiesWithEnclosingClassPresentTwiceTests {
    @Autowired
    lateinit var enclosingContext: ApplicationContext

    @MockkBean
    lateinit var service: ExampleService


    @Test
    fun topLevelTest() {
        assertIsMock(service)
        assertThat(enclosingContext.getBeanNamesForType(ExampleService::class.java)).hasSize(1)

        // The following are prerequisites for the reported regression.
        assertThat(NestedTests::class.java.getSuperclass())
            .isEqualTo(AbstractBaseClassForNestedTests::class.java)
        assertThat(NestedTests::class.java.getEnclosingClass())
            .isEqualTo(AbstractBaseClassForNestedTests::class.java.getEnclosingClass())
            .isEqualTo(javaClass)
    }


    abstract inner class AbstractBaseClassForNestedTests {
        @Test
        fun nestedTest(nestedContext: ApplicationContext?) {
            assertIsMock(service)
            assertThat(enclosingContext).isSameAs(nestedContext)
            assertThat(enclosingContext.getBeanNamesForType(ExampleService::class.java)).hasSize(1)
        }
    }

    @Nested
    inner class NestedTests : AbstractBaseClassForNestedTests()
}
