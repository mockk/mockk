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
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [@MockkBean][MockkBean] where mocks are created
 * for nonexistent beans for a supertype and subtype of that supertype.
 *
 * This test class is designed to reproduce scenarios that previously failed
 * along the lines of the following.
 *
 * BeanNotOfRequiredTypeException: Bean named 'Subtype#0' is expected to be
 * of type 'Subtype' but was actually of type 'Supertype$MockkMock$XHb7Aspo'
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see [gh-34025](https://github.com/spring-projects/spring-framework/issues/34025)
 */
@SpringJUnitConfig
class MockkBeanSuperAndSubtypeIntegrationTests {
    // The declaration order of the following fields is intentional, and prior
    // to fixing gh-34025 this test class consistently failed on JDK 17.
    @MockkBean
    lateinit var subtype: Subtype

    @MockkBean
    lateinit var supertype: Supertype

    @Autowired
    lateinit var supertypes: List<Supertype>

    @Test
    fun bothMocksShouldHaveBeenCreated() {
        assertThat(supertype).isNotSameAs(subtype)
        assertThat(supertypes).hasSize(2)
    }


    interface Supertype

    interface Subtype : Supertype
}
