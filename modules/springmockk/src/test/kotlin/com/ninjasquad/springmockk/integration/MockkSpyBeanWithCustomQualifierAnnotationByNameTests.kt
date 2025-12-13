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
package com.ninjasquad.springmockk.integration

import com.ninjasquad.springmockk.MockkAssertions.assertIsSpy
import com.ninjasquad.springmockk.MockkAssertions.assertMockName
import com.ninjasquad.springmockk.MockkSpyBean
import com.ninjasquad.springmockk.example.ExampleService
import com.ninjasquad.springmockk.example.ExampleServiceCaller
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Tests for [@MockkSpyBean][MockkSpyBean] where the mocked bean is associated
 * with a custom [@Qualifier][Qualifier] annotation and the bean to override
 * is selected by name.
 *
 * @author Sam Brannen
 * @since 6.2.6
 * @see [gh-34646](https://github.com/spring-projects/spring-framework/issues/34646)
 *
 * @see MockkSpyBeanWithCustomQualifierAnnotationByTypeTests
 */
@ExtendWith(SpringExtension::class)
class MockkSpyBeanWithCustomQualifierAnnotationByNameTests {
    @MockkSpyBean(name = "qualifiedService")
    @MyQualifier
    lateinit var service: ExampleService

    @Autowired
    lateinit var caller: ExampleServiceCaller


    @Test
    fun test(context: ApplicationContext) {
        assertIsSpy(service)
        assertMockName(service, "qualifiedService")
        assertThat(service).isInstanceOf(QualifiedService::class.java)

        assertThat(context.getBeanNamesForType(QualifiedService::class.java)).hasSize(1)
        assertThat(context.getBeanNamesForType(ExampleService::class.java)).hasSize(1)
        assertThat(context.getBeanNamesForType(ExampleServiceCaller::class.java)).hasSize(1)

        every { service.greeting() } returns "mock!"
        assertThat(caller.sayGreeting()).isEqualTo("I say mock!")
        verify { service.greeting() }
    }


    @Configuration(proxyBeanMethods = false)
    class Config {
        @Bean
        fun qualifiedService(): QualifiedService {
            return QualifiedService()
        }

        @Bean
        fun myServiceCaller(@MyQualifier service: ExampleService): ExampleServiceCaller {
            return ExampleServiceCaller(service)
        }
    }

    @Qualifier
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class MyQualifier

    @MyQualifier
    class QualifiedService : ExampleService {
        override fun greeting(): String {
            return "Qualified service"
        }
    }
}
