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
import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.ExampleServiceCaller
import com.ninjasquad.springmockk.example.SimpleExampleService
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.MethodMode
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for using [@MockkSpyBean][MockkSpyBean] with
 * [@DirtiesContext][DirtiesContext] and [MethodMode.BEFORE_METHOD].
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 * @see MockkBeanWithDirtiesContextBeforeMethodIntegrationTests
 */
@SpringJUnitConfig
class MockkSpyBeanWithDirtiesContextBeforeMethodIntegrationTests {
    @Autowired
    lateinit var caller: ExampleServiceCaller

    @MockkSpyBean
    lateinit var service: SimpleExampleService

    @Autowired
    lateinit var autowiredService: ExampleService


    @RepeatedTest(2)
    @DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
    fun testSpying() {
        assertThat(service).isSameAs(autowiredService)

        assertThat(caller.sayGreeting()).isEqualTo("I say simple")
        verify { service.greeting() }
    }


    @Configuration(proxyBeanMethods = false)
    @Import(SimpleExampleService::class)
    class Config {
        @Bean
        fun serviceCaller(service: ExampleService): ExampleServiceCaller {
            return ExampleServiceCaller(service)
        }
    }
}
