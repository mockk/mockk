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
import io.mockk.every
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
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
 * Integration tests for [MockkSpyBean] that validate automatic reset
 * of stubbing.
 *
 * @author Simon Baslé
 * @since 6.2
 */
@SpringJUnitConfig
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MockkSpyBeanWithClearIntegrationTests {
    @MockkSpyBean(clear = MockkClear.BEFORE)
    lateinit var service: ExampleService

    @MockkSpyBean(name = "failingExampleServiceFactory", clear = MockkClear.BEFORE)
    lateinit var failingService: FailingExampleService


    @Order(1)
    @Test
    fun beanFirstEstablishingStub(ctx: ApplicationContext) {
        val spy: ExampleService = ctx.getBean("service", ExampleService::class.java)
        every { spy.greeting() } returns "Stubbed hello"

        assertThat(this.service.greeting()).isEqualTo("Stubbed hello")
    }

    @Order(2)
    @Test
    fun beanSecondEnsuringStubReset(ctx: ApplicationContext) {
        assertThat(ctx.getBean("service")).isNotNull().isSameAs(this.service)

        assertThat(this.service.greeting()).`as`("not stubbed")
            .isEqualTo("Production hello")
    }

    @Order(3)
    @Test
    fun factoryBeanFirstEstablishingStub(ctx: ApplicationContext) {
        val spy = ctx.getBean(FailingExampleService::class.java)
        every { spy.greeting() } returns "Stubbed hello"
        assertThat(this.failingService.greeting()).isEqualTo("Stubbed hello")
    }

    @Order(4)
    @Test
    fun factoryBeanSecondEnsuringStubReset(ctx: ApplicationContext) {
        assertThat(ctx.getBean("failingExampleServiceFactory"))
            .isNotNull()
            .isSameAs(this.failingService)

        assertThatIllegalStateException().isThrownBy(this.failingService::greeting)
            .`as`("not stubbed")
            .withMessage("Failed")
    }


    class FailingExampleServiceFactory : FactoryBean<FailingExampleService> {
        override fun getObject() = FailingExampleService()
        override fun getObjectType() = FailingExampleService::class.java
    }

    @Configuration(proxyBeanMethods = false)
    class Config {
        @Bean
        fun service(): ExampleService {
            return RealExampleService("Production hello")
        }

        @Bean
        fun failingExampleServiceFactory(): FailingExampleServiceFactory {
            return FailingExampleServiceFactory()
        }
    }
}
