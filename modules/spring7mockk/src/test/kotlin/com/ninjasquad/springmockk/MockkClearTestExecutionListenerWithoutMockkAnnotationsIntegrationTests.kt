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
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Integration tests for [MockkResetTestExecutionListener] without a
 * [@MockkBean][MockkBean] or [@MockkSpyBean][MockkSpyBean] field.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 * @see MockkClearTestExecutionListenerWithMockkBeanIntegrationTests
 * @see MockkClearStrategiesIntegrationTests
 */
@SpringJUnitConfig
@TestMethodOrder(MethodOrderer.MethodName::class)
open class MockkClearTestExecutionListenerWithoutMockkAnnotationsIntegrationTests {
    @Autowired
    lateinit var context: ApplicationContext

    @Test
    fun test001() {
        val nonSingletonFactoryBean: ExampleService = getMock("nonSingletonFactoryBean")

        every { getMock("none").greeting() } returns "none"
        every { getMock("before").greeting() } returns "before"
        every { getMock("after").greeting() } returns "after"
        every { getMock("singletonFactoryBean").greeting() } returns "singletonFactoryBean"
        every { nonSingletonFactoryBean.greeting() } returns "nonSingletonFactoryBean"

        assertThat(getMock("none").greeting()).isEqualTo("none")
        assertThat(getMock("before").greeting()).isEqualTo("before")
        assertThat(getMock("after").greeting()).isEqualTo("after")
        assertThat(getMock("singletonFactoryBean").greeting()).isEqualTo("singletonFactoryBean")

        // The saved reference should have been mocked.
        assertThat(nonSingletonFactoryBean.greeting()).isEqualTo("nonSingletonFactoryBean")
        // A new reference should have not been mocked.
        assertThat(getMock("nonSingletonFactoryBean").greeting()).isEmpty()

        // getMock("nonSingletonFactoryBean") has been invoked twice in this method.
        assertThat(context.getBean(NonSingletonFactoryBean::class.java).objectInvocations)
            .isEqualTo(2)
    }

    @Test
    fun test002() {
        // Should not have been reset.
        assertThat(getMock("none").greeting()).isEqualTo("none")

        // Should have been reset.
        assertThat(getMock("before").greeting()).isEmpty()
        assertThat(getMock("after").greeting()).isEmpty()
        assertThat(getMock("singletonFactoryBean").greeting()).isEmpty()

        // A non-singleton FactoryBean always creates a new mock instance. Thus,
        // resetting is irrelevant, and the greeting should be null.
        assertThat(getMock("nonSingletonFactoryBean").greeting()).isEmpty()

        // getMock("nonSingletonFactoryBean") has been invoked twice in test001()
        // and once in this method.
        assertThat(context.getBean(NonSingletonFactoryBean::class.java).objectInvocations)
            .isEqualTo(3)
    }

    private fun getMock(name: String): ExampleService {
        return context.getBean(name, ExampleService::class.java)
    }


    @Configuration(proxyBeanMethods = false)
    class Config {
        @Bean
        fun none() = mockk<ExampleService>(relaxed = true)

        @Bean
        fun before() = mockk<ExampleService>(relaxed = true).clear(MockkClear.BEFORE)

        @Bean
        fun after() = mockk<ExampleService>(relaxed = true).clear(MockkClear.AFTER)

        @Bean
        @Lazy
        fun fail(): ExampleService {
            // Spring Boot gh-5870
            throw RuntimeException()
        }

        @Bean
        fun brokenFactoryBean(): BrokenFactoryBean {
            // Spring Boot gh-7270
            return BrokenFactoryBean()
        }

        @Bean
        fun singletonFactoryBean(): WorkingFactoryBean {
            return WorkingFactoryBean()
        }

        @Bean
        fun nonSingletonFactoryBean(): NonSingletonFactoryBean {
            return NonSingletonFactoryBean()
        }
    }

    class BrokenFactoryBean : FactoryBean<String> {
        override fun getObject() = throw IllegalStateException()
        override fun getObjectType() = String::class.java
    }

    class WorkingFactoryBean : FactoryBean<ExampleService> {
        private val service = mockk<ExampleService>(relaxed = true).clear(MockkClear.BEFORE)

        override fun getObject() = service
        override fun getObjectType() = ExampleService::class.java
    }

    class NonSingletonFactoryBean : FactoryBean<ExampleService> {
        var objectInvocations = 0

        override fun getObject(): ExampleService {
            this.objectInvocations++
            return mockk<ExampleService>(relaxed = true).clear(MockkClear.BEFORE)
        }

        override fun getObjectType() = ExampleService::class.java
        override fun isSingleton() = false
    }
}
