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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Integration tests for [@MockkBean][MockkBean] which verify that type-level
 * `@MockkBean` declarations are not discovered more than once when searching
 * a type hierarchy, when an interface is *present* twice in the hierarchy.
 *
 * @author Sam Brannen
 * @since 6.2.7
 * @see MockkBeanNestedAndTypeHierarchiesWithEnclosingClassPresentTwiceTests
 *
 * @see MockkBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests
 *
 * @see [gh-34844](https://github.com/spring-projects/spring-framework/issues/34844)
 */
class MockkBeanWithInterfacePresentTwiceTests : AbstractMockkBeanWithInterfacePresentTwiceTests(),
    MockConfigInterface {

    @Test
    fun test(context: ApplicationContext) {
        assertIsMock(service)
        assertThat(context.getBeanNamesForType(ExampleService::class.java)).hasSize(1)

        // The following are prerequisites for the tested scenario.
        assertThat(javaClass.getInterfaces()).containsExactly(MockConfigInterface::class.java)
        assertThat(javaClass.getSuperclass().getInterfaces())
            .containsExactly(MockConfigInterface::class.java)
    }
}

@MockkBean(types = [ExampleService::class])
interface MockConfigInterface

@ExtendWith(SpringExtension::class)
abstract class AbstractMockkBeanWithInterfacePresentTwiceTests : MockConfigInterface {
    @Autowired
    lateinit var service: ExampleService
}
