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

import com.ninjasquad.springmockk.MockkAssertions.assertIsMock
import com.ninjasquad.springmockk.example.ExampleService
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [@MockkBean][MockkBean] where duplicate mocks
 * are created to replace the same existing bean, selected by-type.
 *
 *
 * In other words, this test class demonstrates how one `@MockkBean`
 * can silently override another `@MockkBean`.
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see [gh-34056](https://github.com/spring-projects/spring-framework/issues/34056)
 *
 * @see MockkBeanDuplicateTypeCreationIntegrationTests
 *
 * @see MockkSpyBeanDuplicateTypeIntegrationTests
 */
@SpringJUnitConfig
class MockkBeanDuplicateTypeReplacementIntegrationTests {
    @MockkBean(clear = MockkClear.AFTER, relaxed = true)
    lateinit var mock1: ExampleService

    @MockkBean(clear = MockkClear.BEFORE, relaxed = true)
    lateinit var mock2: ExampleService

    @Autowired
    lateinit var services: List<ExampleService>

    /**
     * One could argue that we would ideally expect an exception to be thrown when
     * two competing mocks are created to replace the same existing bean; however,
     * we currently only log a warning in such cases.
     *
     * This method therefore asserts the status quo in terms of behavior.
     *
     * And the log can be manually checked to verify that an appropriate
     * warning was logged.
     */
    @Test
    fun onlyOneMockShouldHaveBeenCreated() {
        // Currently logs something similar to the following.
        //
        // WARN - Bean with name 'exampleService' was overridden by multiple handlers:
        // [MockkBeanOverrideHandler@5478ce1e ..., MockkBeanOverrideHandler@5edc70ed ...]

        // Last one wins: there's only one physical mock

        assertThat(services).containsExactly(mock2)
        assertThat(mock1).isSameAs(mock2)

        assertIsMock(mock2)
        assertThat(MockkClear.get(mock2)).`as`("MockkClear").isEqualTo(MockkClear.BEFORE)

        assertThat(mock2.greeting()).isEmpty()
        every { mock2.greeting() } returns "mocked"
        assertThat(mock2.greeting()).isEqualTo("mocked")
    }


    @Configuration
    class Config {
        @Bean
        fun exampleService(): ExampleService {
            return ExampleService { "@Bean" }
        }
    }
}
