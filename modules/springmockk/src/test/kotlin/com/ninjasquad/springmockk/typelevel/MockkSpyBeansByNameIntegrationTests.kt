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
package com.ninjasquad.springmockk.typelevel

import com.ninjasquad.springmockk.MockkAssertions.assertIsNotMock
import com.ninjasquad.springmockk.MockkAssertions.assertIsNotSpy
import com.ninjasquad.springmockk.MockkAssertions.assertIsSpy
import com.ninjasquad.springmockk.MockkSpyBean
import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.RealExampleService
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [@MockkSpyBeans][MockkSpyBeans] and
 * [@MockkSpyBean][MockkSpyBean] declared "by name" at the class level
 * as a repeatable annotation.
 *
 * @author Sam Brannen
 * @since 6.2.3
 * @see [gh-34408](https://github.com/spring-projects/spring-framework/issues/34408)
 *
 * @see MockkSpyBeansByTypeIntegrationTests
 */
@SpringJUnitConfig
@MockkSpyBean(name = "s1", types = [ExampleService::class])
@MockkSpyBean(name = "s2", types = [ExampleService::class])
class MockkSpyBeansByNameIntegrationTests {
    @Autowired
    lateinit var s1: ExampleService

    @Autowired
    lateinit var s2: ExampleService

    @MockkSpyBean(name = "s3")
    lateinit var service3: ExampleService

    @Autowired
    @Qualifier("s4")
    lateinit var service4: ExampleService


    @BeforeEach
    fun configureSpies() {
        every { s1.greeting() } returns "spy 1"
        every { s2.greeting() } returns "spy 2"
        every { service3.greeting() } returns "spy 3"
    }

    @Test
    fun checkSpiesAndStandardBean() {
        assertIsSpy(s1, "s1")
        assertIsSpy(s2, "s2")
        assertIsSpy(service3, "service3")
        assertIsNotMock(service4, "service4")
        assertIsNotSpy(service4, "service4")

        assertThat(s1.greeting()).isEqualTo("spy 1")
        assertThat(s2.greeting()).isEqualTo("spy 2")
        assertThat(service3.greeting()).isEqualTo("spy 3")
        assertThat(service4.greeting()).isEqualTo("prod 4")
    }


    @Configuration
    class Config {
        @Bean
        fun s1(): ExampleService {
            return RealExampleService("prod 1")
        }

        @Bean
        fun s2(): ExampleService {
            return RealExampleService("prod 2")
        }

        @Bean
        fun s3(): ExampleService {
            return RealExampleService("prod 3")
        }

        @Bean
        fun s4(): ExampleService {
            return RealExampleService("prod 4")
        }
    }
}
