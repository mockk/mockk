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
import com.ninjasquad.springmockk.example.RealExampleService
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.bean.override.convention.TestBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for Bean Overrides where a [@MockkBean][MockkBean]
 * overrides a [@TestBean][TestBean] when trying to replace the same existing
 * bean, selected by-type.
 *
 *
 * In other words, this test class demonstrates how one Bean Override can
 * silently override another Bean Override.
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see [gh-34056](https://github.com/spring-projects/spring-framework/issues/34056)
 *
 * @see MockkBeanDuplicateTypeCreationIntegrationTests
 * @see MockkBeanDuplicateTypeReplacementIntegrationTests
 */
@SpringJUnitConfig
class MockkBeanOverridesTestBeanIntegrationTests {
    @TestBean
    lateinit var testService: ExampleService

    @MockkBean(relaxed = true)
    lateinit var mockService: ExampleService

    @Autowired
    lateinit var services: List<ExampleService>


    /**
     * One could argue that we would ideally expect an exception to be thrown when
     * two competing overrides are created to replace the same existing bean; however,
     * we currently only log a warning in such cases.
     *
     * This method therefore asserts the status quo in terms of behavior.
     *
     * And the log can be manually checked to verify that an appropriate
     * warning was logged.
     */
    @Test
    fun mockkBeanShouldOverrideTestBean() {
        // Currently logs something similar to the following.
        //
        // WARN - Bean with name 'exampleService' was overridden by multiple handlers:
        // [TestBeanOverrideHandler@770beef5 ..., MockkBeanOverrideHandler@6dd1f638 ...]

        // Last override wins...

        assertThat(services).containsExactly(mockService)
        assertThat(testService).isSameAs(mockService)

        assertIsMock(mockService)

        assertThat(mockService.greeting()).isEqualTo("")
        every { mockService.greeting() } returns "mocked"
        assertThat(mockService.greeting()).isEqualTo("mocked")
    }


    @Configuration
    class Config {
        @Bean
        fun exampleService(): ExampleService {
            return ExampleService { "@Bean" }
        }
    }

    companion object {
        @JvmStatic
        fun testService(): ExampleService {
            return RealExampleService("@TestBean")
        }
    }
}
