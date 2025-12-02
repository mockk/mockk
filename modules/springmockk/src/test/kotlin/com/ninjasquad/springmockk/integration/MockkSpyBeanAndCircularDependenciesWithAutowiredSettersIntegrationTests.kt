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

import com.ninjasquad.springmockk.MockkSpyBean
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.aot.DisabledInAotMode
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Tests that [@MockkSpyBean][MockkSpyBean] can be used to replace an
 * existing bean with circular dependencies with [@Autowired][Autowired]
 * setter methods.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 * @see MockkSpyBeanAndCircularDependenciesWithLazyResolutionProxyIntegrationTests
 */
@SpringJUnitConfig
@DisabledInAotMode("Circular dependencies cannot be resolved in AOT mode unless a @Lazy resolution proxy is used")
class MockkSpyBeanAndCircularDependenciesWithAutowiredSettersIntegrationTests {
    @MockkSpyBean
    lateinit var one: One

    @Autowired
    lateinit var two: Two


    @Test
    fun beanWithCircularDependenciesCanBeSpied() {
        two.callOne()
        verify { one.doSomething() }
    }


    @Configuration
    @Import(One::class, Two::class)
    class Config

    class One {
        @Suppress("unused")
        private var two: Two? = null

        @Autowired
        fun setTwo(two: Two?) {
            this.two = two
        }

        fun doSomething() {
        }
    }

    class Two {
        private var one: One? = null

        @Autowired
        fun setOne(one: One) {
            this.one = one
        }

        fun callOne() {
            this.one!!.doSomething()
        }
    }
}
