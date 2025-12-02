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
import com.ninjasquad.springmockk.example.CustomQualifier
import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.RealExampleService
import io.mockk.every
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.NoUniqueBeanDefinitionException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.core.annotation.Order
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [MockkBean] that use by-type lookup.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
class MockkBeanByTypeLookupIntegrationTests {
    @MockkBean
    lateinit var serviceIsNotABean: AnotherService

    @MockkBean
    lateinit var anyNameForService: ExampleService

    @MockkBean
    @Qualifier("prefer")
    lateinit var ambiguous: StringBuilder

    @MockkBean
    @CustomQualifier
    lateinit var ambiguousMeta: StringBuilder

    @MockkBean
    lateinit var yetAnotherService: YetAnotherService


    @Test
    fun mockIsCreatedWhenNoCandidateIsFound() {
        assertIsMock(serviceIsNotABean)

        every { serviceIsNotABean.hello() } returns "Mocked hello"

        assertThat(serviceIsNotABean.hello()).isEqualTo("Mocked hello")
        verifyAll { serviceIsNotABean.hello() }
    }

    @Test
    fun overrideIsFoundByType(ctx: ApplicationContext) {
        assertThat(anyNameForService)
            .satisfies(MockkAssertions::assertIsMock)
            .isSameAs(ctx.getBean("example"))
            .isSameAs(ctx.getBean(ExampleService::class.java))

        every { anyNameForService.greeting() } returns "Mocked greeting"

        assertThat(anyNameForService.greeting()).isEqualTo("Mocked greeting")
        verifyAll { anyNameForService.greeting() }
    }

    @Test
    fun overrideIsFoundByTypeAndDisambiguatedByQualifier(ctx: ApplicationContext) {
        assertThat(ambiguous)
            .satisfies(MockkAssertions::assertIsMock)
            .isSameAs(ctx.getBean("ambiguous2"))

        assertThatExceptionOfType(NoUniqueBeanDefinitionException::class.java)
            .isThrownBy { ctx.getBean(StringBuilder::class.java) }
            .satisfies({ ex: NoUniqueBeanDefinitionException ->
                assertThat(ex.getBeanNamesFound()).containsOnly("ambiguous1", "ambiguous2")
            })

        every { ambiguous.length } returns 0
        every { ambiguous.substring(0) } returns null

        assertThat(ambiguous).isEmpty()
        assertThat(ambiguous.substring(0)).isNull()
        verifyAll {
            ambiguous.length
            ambiguous.substring(any())
        }
    }

    @Test
    fun overrideIsFoundByTypeAndDisambiguatedByMetaQualifier(ctx: ApplicationContext) {
        assertThat(ambiguousMeta)
            .satisfies(MockkAssertions::assertIsMock)
            .isSameAs(ctx.getBean("ambiguous1"))

        assertThatExceptionOfType(NoUniqueBeanDefinitionException::class.java)
            .isThrownBy { ctx.getBean(StringBuilder::class.java) }
            .satisfies({ ex: NoUniqueBeanDefinitionException ->
                assertThat(ex.getBeanNamesFound()).containsOnly("ambiguous1", "ambiguous2")
            })

        every { ambiguousMeta.length } returns 0
        every { ambiguousMeta.substring(0) } returns null

        assertThat(ambiguousMeta).isEmpty()
        assertThat(ambiguousMeta.substring(0)).isNull()
        verifyAll {
            ambiguousMeta.length
            ambiguousMeta.substring(any())
        }
    }

    @Test
    fun overrideIsFoundByTypeForPrototype(ctx: ConfigurableApplicationContext) {
        assertThat(yetAnotherService)
            .satisfies(MockkAssertions::assertIsMock)
            .isSameAs(ctx.getBean("YAS"))
            .isSameAs(ctx.getBean(YetAnotherService::class.java))
        assertThat(ctx.getBeanFactory().getBeanDefinition("YAS").isSingleton()).`as`("isSingleton").isTrue()

        every { yetAnotherService.hello() } returns "Mocked greeting"

        assertThat(yetAnotherService.hello()).isEqualTo("Mocked greeting")
        verifyAll { yetAnotherService.hello() }
    }


    interface AnotherService {
        fun hello(): String
    }

    fun interface YetAnotherService {
        fun hello(): String
    }

    @Configuration(proxyBeanMethods = false)
    class Config {
        @Bean("example")
        fun bean1(): ExampleService? {
            return RealExampleService("Production hello")
        }

        @Bean("ambiguous1")
        @Order(1)
        @CustomQualifier
        fun bean2(): StringBuilder {
            return StringBuilder("bean2")
        }

        @Bean("ambiguous2")
        @Order(2)
        @Qualifier("prefer")
        fun bean3(): StringBuilder {
            return StringBuilder("bean3")
        }

        @Bean("YAS")
        @Scope("prototype")
        fun bean4(): YetAnotherService {
            return YetAnotherService { "Production Hello" }
        }
    }
}
