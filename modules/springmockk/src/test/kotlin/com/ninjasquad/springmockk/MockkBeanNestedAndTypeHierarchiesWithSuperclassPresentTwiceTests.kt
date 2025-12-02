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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Integration tests for [@MockkBean][MockkBean] which verify that
 * `@MockkBean` fields are not discovered more than once when searching
 * intertwined enclosing class hierarchies and type hierarchies, when a superclass
 * is *present* twice in the intertwined hierarchies.
 *
 * @author Sam Brannen
 * @since 6.2.7
 * @see MockkBeanNestedAndTypeHierarchiesWithEnclosingClassPresentTwiceTests
 *
 * @see MockkBeanWithInterfacePresentTwiceTests
 *
 * @see [gh-34844](https://github.com/spring-projects/spring-framework/issues/34844)
 */
class MockkBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests
    : AbstractMockkBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests() {

    @Test
    override fun topLevelTest() {
        super.topLevelTest()

        // The following are prerequisites for the reported regression.
        assertThat(NestedTests::class.java.getSuperclass())
            .isEqualTo(AbstractBaseClassForNestedTests::class.java)
        assertThat(NestedTests::class.java.getEnclosingClass())
            .isEqualTo(javaClass)
        assertThat(NestedTests::class.java.getEnclosingClass().getSuperclass())
            .isEqualTo(AbstractBaseClassForNestedTests::class.java.getEnclosingClass())
            .isEqualTo(javaClass.getSuperclass())
    }


    @Nested
    inner class NestedTests : AbstractBaseClassForNestedTests()
}
