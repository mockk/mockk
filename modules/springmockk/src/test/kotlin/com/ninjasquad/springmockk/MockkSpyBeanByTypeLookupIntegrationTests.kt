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

import com.ninjasquad.springmockk.example.CustomQualifier
import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.RealExampleService
import io.mockk.verifyAll
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.core.annotation.Order
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [MockkSpyBean] that use by-type lookup.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
class MockkSpyBeanByTypeLookupIntegrationTests {
    @MockkSpyBean
    lateinit var anyNameForService: ExampleService

    @MockkSpyBean
    @Qualifier("prefer")
    lateinit var ambiguous: StringHolder

    @MockkSpyBean
    @CustomQualifier
    lateinit var ambiguousMeta: StringHolder

    @MockkSpyBean
    lateinit var prototypeService: AnotherService


    @Test
    fun overrideIsFoundByType(ctx: ApplicationContext) {
        assertThat(anyNameForService)
            .satisfies(MockkAssertions::assertIsSpy)
            .isSameAs(ctx.getBean("example"))
            .isSameAs(ctx.getBean(ExampleService::class.java))

        assertThat(anyNameForService.greeting()).isEqualTo("Production hello")
        verifyAll { anyNameForService.greeting() }
    }

    @Test
    fun overrideIsFoundByTypeAndDisambiguatedByQualifier(ctx: ApplicationContext) {
        assertThat(ambiguous)
            .satisfies(MockkAssertions::assertIsSpy)
            .isSameAs(ctx.getBean("ambiguous2"))

        assertThatException()
            .isThrownBy { ctx.getBean(StringHolder::class.java) }
            .withMessageEndingWith("but found 2: ambiguous1,ambiguous2")

        assertThat(ambiguous.value).isEqualTo("bean3")
        assertThat(ambiguous.size()).isEqualTo(5)
        verifyAll {
            ambiguous.value
            ambiguous.size()
        }
    }

    @Test
    fun overrideIsFoundByTypeAndDisambiguatedByMetaQualifier(ctx: ApplicationContext) {
        assertThat(ambiguousMeta)
            .satisfies(MockkAssertions::assertIsSpy)
            .isSameAs(ctx.getBean("ambiguous1"))

        assertThatException()
            .isThrownBy { ctx.getBean(StringHolder::class.java) }
            .withMessageEndingWith("but found 2: ambiguous1,ambiguous2")

        assertThat(ambiguousMeta.value).isEqualTo("bean2")
        assertThat(ambiguousMeta.size()).isEqualTo(5)
        verifyAll {
            ambiguousMeta.value
            ambiguousMeta.size()
        }
    }

    @Test
    fun overrideIsFoundByTypeForPrototype(ctx: ConfigurableApplicationContext) {
        assertThat(prototypeService)
            .satisfies(MockkAssertions::assertIsSpy)
            .isSameAs(ctx.getBean("anotherService"))
            .isSameAs(ctx.getBean(AnotherService::class.java))
        assertThat(ctx.getBeanFactory().getBeanDefinition("anotherService").isSingleton())
            .`as`("isSingleton").isTrue()

        assertThat(prototypeService.hello()).isEqualTo("Production Hello")
        verifyAll {
            prototypeService.hello()
        }

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
        fun bean2(): StringHolder {
            return StringHolder("bean2")
        }

        @Bean("ambiguous2")
        @Order(2)
        @Qualifier("prefer")
        fun bean3(): StringHolder {
            return StringHolder("bean3")
        }

        @Bean("anotherService")
        @Scope("prototype")
        fun bean4(): AnotherService {
            return DefaultAnotherService("Production Hello")
        }
    }

    class StringHolder(val value: String) {
        fun size(): Int {
            return value.length
        }
    }

    interface AnotherService {
        fun hello(): String
    }

    data class DefaultAnotherService(val message: String) : AnotherService {
        override fun hello(): String {
            return message
        }
    }
}
