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

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.ExampleServiceCaller
import com.ninjasquad.springmockk.example.FailingExampleService
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Tests for [@MockkBean][MockkBean] used in combination with scoped-proxy
 * targets.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see [gh-5724](https://github.com/spring-projects/spring-boot/issues/5724)
 */
@ExtendWith(SpringExtension::class)
class MockkBeanAndScopedProxyIntegrationTests {
    @MockkBean
    lateinit var service: ExampleService

    // The ExampleService mock should replace the scoped-proxy FailingExampleService
    // created in the @Configuration class.

    @Autowired
    lateinit var serviceCaller: ExampleServiceCaller


    @BeforeEach
    fun configureServiceMock() {
        every { service.greeting() } returns "mock"
    }

    @Test
    fun testMocking() {
        assertThat(serviceCaller.sayGreeting()).isEqualTo("I say mock")
    }


    @Configuration(proxyBeanMethods = false)
    class Config {
        @Bean
        @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
        fun exampleService(): ExampleService? {
            return FailingExampleService()
        }

        @Bean
        fun serviceCaller(service: ExampleService): ExampleServiceCaller {
            return ExampleServiceCaller(service)
        }
    }
}
