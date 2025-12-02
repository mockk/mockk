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
import com.ninjasquad.springmockk.MockkSpyBean
import com.ninjasquad.springmockk.example.ExampleService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Integration tests that verify support for [@MockkBean][MockkBean]
 * and [@MockkSpyBean][MockkSpyBean] when the [MockKExtension]
 * is registered alongside the [SpringExtension].
 *
 * This test class currently verifies explicit support for [@MockK][MockK],
 * but we may extend the scope of this test class in the future.
 *
 * @author Sam Brannen
 * @since 6.2
 */
@ExtendWith(MockKExtension::class)
@ExtendWith(SpringExtension::class)
class SpringExtensionAndMockkExtensionIntegrationTests {
    @MockkSpyBean
    lateinit var registrationService: RegistrationService

    @MockK
    lateinit var exampleService: ExampleService

    @Test
    fun test() {
        every { exampleService.greeting() } returns "Hello"

        assertThat(registrationService.registerUser("Duke", exampleService)).isEqualTo("Hello Duke")
        verify {
            registrationService.registerUser("Duke", exampleService)
            exampleService.greeting()
        }
    }

    @Configuration
    class Config {
        @Bean
        fun registrationService(): RegistrationService {
            return RegistrationService()
        }
    }

    class RegistrationService {
        fun registerUser(name: String, exampleService: ExampleService): String {
            return "${exampleService.greeting()} $name"
        }
    }
}
