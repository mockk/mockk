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

import com.ninjasquad.springmockk.MockkAssertions.assertIsMock
import com.ninjasquad.springmockk.MockkAssertions.assertIsNotSpy
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.integration.MockkBeanUsedDuringApplicationContextRefreshIntegrationTests.ContextRefreshedEventListener
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [@MockkBean][MockkBean] used during
 * `ApplicationContext` refresh.
 *
 * @author Sam Brannen
 * @author Yanming Zhou
 * @since 6.2.1
 * @see MockkSpyBeanUsedDuringApplicationContextRefreshIntegrationTests
 */
@SpringJUnitConfig(ContextRefreshedEventListener::class)
class MockkBeanUsedDuringApplicationContextRefreshIntegrationTests {
    @MockkBean(relaxUnitFun = true)
    lateinit var eventProcessor: ContextRefreshedEventProcessor


    @Test
    fun test() {
        assertIsMock(eventProcessor)
        assertIsNotSpy(eventProcessor)

        // Ensure that the mock was invoked during ApplicationContext refresh
        // and has not been reset in the interim.
        verify { eventProcessor.process(any<ContextRefreshedEvent>()) }
    }


    interface ContextRefreshedEventProcessor {
        fun process(event: ContextRefreshedEvent?)
    }

    // MUST be annotated with @Component, due to EventListenerMethodProcessor.isSpringContainerClass().
    @Component
    data class ContextRefreshedEventListener(val contextRefreshedEventProcessor: ContextRefreshedEventProcessor) {
        @EventListener
        fun onApplicationEvent(event: ContextRefreshedEvent?) {
            this.contextRefreshedEventProcessor.process(event)
        }
    }
}
