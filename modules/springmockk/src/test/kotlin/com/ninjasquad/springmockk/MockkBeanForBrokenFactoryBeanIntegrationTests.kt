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

import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

/**
 * Test [@MockkBean][MockkBean] for a [FactoryBean] that is
 * "broken" or not able to be eagerly initialized.
 *
 * @author Sam Brannen
 * @author Simon Baslé
 */
@SpringJUnitConfig
class MockkBeanForBrokenFactoryBeanIntegrationTests {
    @MockkBean
    lateinit var testBean: TestBean


    @Test
    fun beanReturnedByFactoryIsMocked(@Autowired autowiredTestBean: TestBean) {
        assertThat(autowiredTestBean).isSameAs(testBean)

        every { testBean.hello() } returns "mock"

        assertThat(testBean.hello()).isEqualTo("mock")
    }


    @Configuration(proxyBeanMethods = false)
    class Config {
        @Bean
        fun testFactoryBean(): TestFactoryBean {
            return TestFactoryBean()
        }
    }

    class TestFactoryBean : FactoryBean<TestBean> {
        init {
            throw BeanCreationException("simulating missing dependencies")
        }

        override fun getObject() = TestBean { "prod" }
        override fun getObjectType() = TestBean::class.java
    }

    fun interface TestBean {
        fun hello(): String?
    }
}
