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

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.integration.AbstractMockkBeanAndGenericsIntegrationTests.Something
import com.ninjasquad.springmockk.integration.AbstractMockkBeanAndGenericsIntegrationTests.Thing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Abstract base class for tests for [@MockkBean][MockkBean] with generics.
 *
 * @param <T> type of thing
 * @param <S> type of something
 * @author Madhura Bhave
 * @author Sam Brannen
 * @since 6.2
 * @see MockkBeanAndGenericsIntegrationTests</S></T>
 */
@SpringJUnitConfig
abstract class AbstractMockkBeanAndGenericsIntegrationTests<T : Thing<S>, S : Something> {
    @Autowired
    lateinit var thing: T

    @MockkBean
    lateinit var something: S


    open class Something {
        fun speak(): String {
            return "Hi"
        }
    }

    class SomethingImpl : Something()

    abstract class Thing<S : Something> {
        @Autowired
        lateinit var something: S
    }

    class ThingImpl : Thing<SomethingImpl>()

    @Configuration(proxyBeanMethods = false)
    class Config {
        @Bean
        fun thing(): ThingImpl {
            return ThingImpl()
        }
    }
}
