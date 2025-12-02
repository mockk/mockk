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

import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Verifies proper reset of mocks when a [@MockkBean][MockkBean] field
 * is declared in the enclosing class of a [@Nested][Nested] test class.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 */
@ExtendWith(SpringExtension::class) // TODO Remove @ContextConfiguration declaration.
// @ContextConfiguration is currently required due to a bug in the TestContext framework.
// See https://github.com/spring-projects/spring-framework/issues/31456
@ContextConfiguration
class MockkBeanNestedTests {
    @MockkBean(relaxUnitFun = true)
    lateinit var action: Runnable

    @Autowired
    lateinit var task: Task

    @Test
    fun mockWasInvokedOnce() {
        task.execute()
        verify { action.run() }
    }

    @Test
    fun mockWasInvokedTwice() {
        task.execute()
        task.execute()
        verify(exactly = 2) { action.run() }
    }

    @Nested
    inner class MockkBeanFieldInEnclosingClassTests {
        @Test
        fun mockWasInvokedOnce() {
            task.execute()
            verify { action.run() }
        }

        @Test
        fun mockWasInvokedTwice() {
            task.execute()
            task.execute()
            verify(exactly = 2) { action.run() }
        }
    }

    @JvmRecord
    data class Task(val action: Runnable) {
        fun execute() {
            this.action.run()
        }
    }

    @Configuration(proxyBeanMethods = false)
    class TestConfiguration {
        @Bean
        fun task(action: Runnable): Task {
            return Task(action)
        }
    }
}
