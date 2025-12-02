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

import com.ninjasquad.springmockk.MockkBeanManuallyRegisteredSingletonTests.SingletonRegistrar
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Verifies support for overriding a manually registered singleton bean with
 * [@MockkBean][MockkBean].
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig(initializers = [SingletonRegistrar::class])
class MockkBeanManuallyRegisteredSingletonTests {
    @MockkBean
    lateinit var messageService: MessageService

    @Test
    fun test() {
        every { messageService.message } returns "override"
        assertThat(messageService.message).isEqualTo("override")
    }

    class SingletonRegistrar : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            applicationContext.getBeanFactory().registerSingleton("messageService", MessageService())
        }
    }

    class MessageService {
        val message: String
            get() = "production"
    }
}
