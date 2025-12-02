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

import com.ninjasquad.springmockk.MockkAssertions.assertIsSpy
import com.ninjasquad.springmockk.MockkSpyBean
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [@MockkSpyBean][MockkSpyBean] used during
 * `ApplicationContext` refresh.
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see MockkBeanUsedDuringApplicationContextRefreshIntegrationTests
 */
@SpringJUnitConfig
class MockkSpyBeanUsedDuringApplicationContextRefreshIntegrationTests {
    @MockkSpyBean
    lateinit var eventProcessor: ContextRefreshedEventProcessor


    @Test
    fun test() {
        assertIsSpy(eventProcessor)

        // Ensure that the spy was invoked during ApplicationContext refresh
        // and has not been reset in the interim.
        verify { eventProcessor.process(refEq(contextRefreshedEvent!!)) }
    }


    @Configuration
    @Import(ContextRefreshedEventListener::class)
    class Config {
        @Bean
        fun eventProcessor(): ContextRefreshedEventProcessor {
            // Cannot be a lambda expression, since Mockk cannot create a spy for a lambda.
            return object : ContextRefreshedEventProcessor {
                override fun process(event: ContextRefreshedEvent?) {
                    contextRefreshedEvent = event
                }
            }
        }
    }

    interface ContextRefreshedEventProcessor {
        fun process(event: ContextRefreshedEvent?)
    }

    // MUST be annotated with @Component, due to EventListenerMethodProcessor.isSpringContainerClass().
    @Component
    data class ContextRefreshedEventListener(val contextRefreshedEventProcessor: ContextRefreshedEventProcessor?) {
        @EventListener
        fun onApplicationEvent(event: ContextRefreshedEvent?) {
            this.contextRefreshedEventProcessor!!.process(event)
        }
    }

    companion object {
        var contextRefreshedEvent: ContextRefreshedEvent? = null

        @AfterAll
        @JvmStatic
        fun clearStaticField() {
            contextRefreshedEvent = null
        }
    }
}
