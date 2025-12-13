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
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.test.context.junit.jupiter.SpringExtensionConfig
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [MockkBean] that use by-name lookup with test class
 * [ExtensionContextScope].
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
@SpringExtensionConfig(useTestClassScopedExtensionContext = true)
class MockkBeanByNameLookupTestClassScopedExtensionContextIntegrationTests {
    @MockkBean("field")
    lateinit var field: ExampleService

    @MockkBean("nonExistingBean")
    lateinit var nonExisting: ExampleService

    @MockkBean("prototypeScoped")
    lateinit var prototypeScoped: ExampleService

    @Test
    fun fieldAndRenamedFieldHaveSameOverride(ctx: ApplicationContext) {
        every { field.greeting() } returns null

        assertThat(ctx.getBean("field"))
            .isInstanceOf(ExampleService::class.java)
            .satisfies(MockkAssertions::assertIsMock)
            .isSameAs(field)

        assertThat(field.greeting()).`as`("mocked greeting").isNull()
    }

    @Test
    fun fieldIsMockedWhenNoOriginalBean(ctx: ApplicationContext) {
        every { nonExisting.greeting() } returns null

        assertThat(ctx.getBean("nonExistingBean"))
            .isInstanceOf(ExampleService::class.java)
            .satisfies(MockkAssertions::assertIsMock)
            .isSameAs(nonExisting)

        assertThat(nonExisting.greeting()).`as`("mocked greeting").isNull()
    }

    @Test
    fun fieldForPrototypeHasOverride(ctx: ConfigurableApplicationContext) {
        every { prototypeScoped.greeting() } returns null

        assertThat(ctx.getBean("prototypeScoped"))
            .isInstanceOf(ExampleService::class.java)
            .satisfies(MockkAssertions::assertIsMock)
            .isSameAs(prototypeScoped)
        assertThat(ctx.getBeanFactory().getBeanDefinition("prototypeScoped").isSingleton())
            .`as`("isSingleton").isTrue()

        assertThat(prototypeScoped.greeting()).`as`("mocked greeting").isNull()
    }


    @Nested
    @DisplayName("With @MockkBean in enclosing class and in @Nested class")
    inner class MockkBeanNestedTests {
        @Autowired
        @Qualifier("field")
        lateinit var localField: ExampleService

        @Autowired
        @Qualifier("nonExistingBean")
        lateinit var localNonExisting: ExampleService

        @MockkBean("nestedField")
        lateinit var nestedField: ExampleService

        @MockkBean("nestedNonExistingBean")
        lateinit var nestedNonExisting: ExampleService


        @Test
        fun fieldAndRenamedFieldHaveSameOverride(ctx: ApplicationContext) {
            every { localField.greeting() } returns null

            assertThat(ctx.getBean("field"))
                .isInstanceOf(ExampleService::class.java)
                .satisfies(MockkAssertions::assertIsMock)
                .isSameAs(localField)

            assertThat(localField.greeting()).`as`("mocked greeting").isNull()
        }

        @Test
        fun fieldIsMockedWhenNoOriginalBean(ctx: ApplicationContext) {
            every { localNonExisting.greeting() } returns null

            assertThat(ctx.getBean("nonExistingBean"))
                .isInstanceOf(ExampleService::class.java)
                .satisfies(MockkAssertions::assertIsMock)
                .isSameAs(localNonExisting)

            assertThat(localNonExisting.greeting()).`as`("mocked greeting").isNull()
        }

        @Test
        fun nestedFieldAndRenamedFieldHaveSameOverride(ctx: ApplicationContext) {
            assertThat(ctx.getBean("nestedField"))
                .isInstanceOf(ExampleService::class.java)
                .satisfies(MockkAssertions::assertIsMock)
                .isSameAs(nestedField)
        }

        @Test
        fun nestedFieldIsMockedWhenNoOriginalBean(ctx: ApplicationContext) {
            assertThat(ctx.getBean("nestedNonExistingBean"))
                .isInstanceOf(ExampleService::class.java)
                .satisfies(MockkAssertions::assertIsMock)
                .isSameAs(nestedNonExisting)
        }
    }


    @Configuration(proxyBeanMethods = false)
    class Config {
        @Bean("field")
        fun bean1(): ExampleService? {
            return RealExampleService("Hello Field")
        }

        @Bean("nestedField")
        fun bean2(): ExampleService? {
            return RealExampleService("Hello Nested Field")
        }

        @Bean("prototypeScoped")
        @Scope("prototype")
        fun bean3(): ExampleService? {
            return RealExampleService("Hello Prototype Field")
        }
    }
}
