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

import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.RealExampleService
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [MockkSpyBean] that use by-name lookup with
 * [ExtensionContextScope.TEST_METHOD].
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2.13
 */
@SpringJUnitConfig
class MockkSpyBeanByNameLookupTestMethodScopedExtensionContextIntegrationTests {
    @MockkSpyBean("field1")
    lateinit var field: ExampleService


    @Test
    fun fieldHasOverride(ctx: ApplicationContext) {
        assertThat(ctx.getBean("field1"))
            .isInstanceOf(ExampleService::class.java)
            .satisfies(MockkAssertions::assertIsSpy)
            .isSameAs(field)

        assertThat(field.greeting()).isEqualTo("bean1")
    }


    @Nested
    @DisplayName("With @MockkSpyBean in enclosing class and in @Nested class")
    inner class MockkSpyBeanNestedTests {
        @Autowired
        @Qualifier("field1")
        lateinit var localField: ExampleService

        @MockkSpyBean("field2")
        lateinit var nestedField: ExampleService

        @Test
        fun fieldHasOverride(ctx: ApplicationContext) {
            assertThat(ctx.getBean("field1"))
                .isInstanceOf(ExampleService::class.java)
                .satisfies(MockkAssertions::assertIsSpy)
                .isSameAs(localField)

            assertThat(localField.greeting()).isEqualTo("bean1")
        }

        @Test
        fun nestedFieldHasOverride(ctx: ApplicationContext) {
            assertThat(ctx.getBean("field2"))
                .isInstanceOf(ExampleService::class.java)
                .satisfies(MockkAssertions::assertIsSpy)
                .isSameAs(nestedField)

            assertThat(nestedField.greeting()).isEqualTo("bean2")
        }
    }


    @Configuration(proxyBeanMethods = false)
    class Config {
        @Bean("field1")
        fun bean1(): ExampleService? {
            return RealExampleService("bean1")
        }

        @Bean("field2")
        fun bean2(): ExampleService? {
            return RealExampleService("bean2")
        }
    }
}
