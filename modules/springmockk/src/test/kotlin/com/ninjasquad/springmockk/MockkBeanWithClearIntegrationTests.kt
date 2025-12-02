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
import com.ninjasquad.springmockk.example.FailingExampleService
import com.ninjasquad.springmockk.example.RealExampleService
import io.mockk.MockKException
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [MockkBean] that validate automatic reset
 * of stubbing.
 *
 * @author Simon Baslé
 * @since 6.2
 */
@SpringJUnitConfig
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MockkBeanWithClearIntegrationTests {
    @MockkBean(clear = MockkClear.BEFORE)
    lateinit var service: ExampleService

    @MockkBean(clear = MockkClear.BEFORE)
    lateinit var failingService: FailingExampleService

    @Order(1)
    @Test
    fun beanFirstEstablishingMock(ctx: ApplicationContext) {
        val mock: ExampleService = ctx.getBean("service", ExampleService::class.java)
        every { mock.greeting() } returns "Mocked hello"

        assertThat(this.service.greeting()).isEqualTo("Mocked hello")
    }

    @Order(2)
    @Test
    fun beanSecondEnsuringMockReset(ctx: ApplicationContext) {
        assertThat(ctx.getBean("service")).isNotNull().isSameAs(this.service)

        assertThatExceptionOfType(MockKException::class.java).`as`("not stubbed").isThrownBy { this.service.greeting() }
            .withMessageContaining("no answer found")
    }

    @Order(3)
    @Test
    fun factoryBeanFirstEstablishingMock(ctx: ApplicationContext) {
        val mock: FailingExampleService = ctx.getBean(FailingExampleService::class.java)
        every { mock.greeting() } returns "Mocked hello"

        assertThat(this.failingService.greeting()).isEqualTo("Mocked hello")
    }

    @Order(4)
    @Test
    fun factoryBeanSecondEnsuringMockReset(ctx: ApplicationContext) {
        assertThat(ctx.getBean("factory")).isNotNull().isSameAs(this.failingService)

        assertThatExceptionOfType(MockKException::class.java).`as`("not stubbed").isThrownBy { this.failingService.greeting() }
            .withMessageContaining("no answer found")
    }

    class FailingExampleServiceFactory : FactoryBean<FailingExampleService> {
        override fun getObject() = FailingExampleService()
        override fun getObjectType() = FailingExampleService::class.java
    }

    @Configuration(proxyBeanMethods = false)
    class Config {
        @Bean("service")
        fun bean1(): ExampleService? {
            return RealExampleService("Production hello")
        }

        @Bean("factory")
        fun factory(): FailingExampleServiceFactory {
            return FailingExampleServiceFactory()
        }
    }
}
