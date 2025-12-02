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

import com.ninjasquad.springmockk.MockkAssertions.assertIsMock
import com.ninjasquad.springmockk.MockkAssertions.assertIsNotMock
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.example.ExampleService
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
 * Integration tests for [@MockkBeans][MockkBeans] and
 * [@MockkBean][MockkBean] declared "by name" at the class level as a
 * repeatable annotation.
 *
 * @author Sam Brannen
 * @since 6.2.2
 * @see [gh-33925](https://github.com/spring-projects/spring-framework/issues/33925)
 *
 * @see MockkBeansByTypeIntegrationTests
 */
@SpringJUnitConfig
@MockkBean(name = "s1", types = [ExampleService::class])
@MockkBean(name = "s2", types = [ExampleService::class])
class MockkBeansByNameIntegrationTests {
    @Autowired
    lateinit var s1: ExampleService

    @Autowired
    lateinit var s2: ExampleService

    @MockkBean(name = "s3")
    lateinit var service3: ExampleService

    @Autowired
    @Qualifier("s4")
    lateinit var service4: ExampleService


    @BeforeEach
    fun configureMocks() {
        every { s1.greeting() } returns "mock 1"
        every { s2.greeting() } returns "mock 2"
        every { service3.greeting() } returns "mock 3"
    }

    @Test
    fun checkMocksAndStandardBean() {
        assertIsMock(s1, "s1")
        assertIsMock(s2, "s2")
        assertIsMock(service3, "service3")
        assertIsNotMock(service4, "service4")

        assertThat(s1.greeting()).isEqualTo("mock 1")
        assertThat(s2.greeting()).isEqualTo("mock 2")
        assertThat(service3.greeting()).isEqualTo("mock 3")
        assertThat(service4.greeting()).isEqualTo("prod 4")
    }


    @Configuration
    class Config {
        @Bean
        fun s1(): ExampleService? {
            return ExampleService { "prod 1" }
        }

        @Bean
        fun s2(): ExampleService? {
            return ExampleService { "prod 2" }
        }

        @Bean
        fun s3(): ExampleService? {
            return ExampleService { "prod 3" }
        }

        @Bean
        fun s4(): ExampleService? {
            return ExampleService { "prod 4" }
        }
    }
}
