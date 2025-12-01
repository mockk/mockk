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

import com.ninjasquad.springmockk.MockkAssertions.assertIsMock
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Tests for [@MockkBean][MockkBean] where the mocked interface has an
 * [@Async][Async] method.
 *
 * @author Sam Brannen
 * @author Andy Wilkinson
 * @since 6.2
 */
@ExtendWith(SpringExtension::class)
class MockkBeanAndAsyncInterfaceMethodIntegrationTests {
    @MockkBean
    lateinit var transformer: Transformer

    @Autowired
    lateinit var service: MyService


    @Test
    @Throws(Exception::class)
    fun mockedMethodsAreNotAsync() {
        assertThat(AopUtils.isAopProxy(transformer)).`as`("is Spring AOP proxy").isFalse()
        assertIsMock(transformer)

        every { transformer.transform("foo") } returns CompletableFuture.completedFuture("bar")
        assertThat(service.transform("foo")).isEqualTo("result: bar")
    }


    fun interface Transformer {
        @Async
        fun transform(input: String): CompletableFuture<String>
    }

    @JvmRecord
    data class MyService(val transformer: Transformer) {
        @Throws(Exception::class)
        fun transform(input: String): String {
            return "result: " + this.transformer.transform(input).get()
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    class Config {
        @Bean
        fun transformer(): Transformer {
            return Transformer { input: String ->
                CompletableFuture.completedFuture(
                    input.uppercase(Locale.getDefault())
                )
            }
        }

        @Bean
        fun myService(transformer: Transformer): MyService {
            return MyService(transformer)
        }
    }
}
