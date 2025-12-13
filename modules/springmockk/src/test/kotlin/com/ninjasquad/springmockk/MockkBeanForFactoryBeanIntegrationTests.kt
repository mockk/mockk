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
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Test [@MockkBean][MockkBean] for a factory bean configuration.
 *
 * @author Simon Baslé
 */
@SpringJUnitConfig
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MockkBeanForFactoryBeanIntegrationTests {
    @MockkBean(relaxed = true)
    private lateinit var testBean: TestBean

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Order(1)
    @Test
    fun beanReturnedByFactoryIsMocked() {
        val bean = this.applicationContext.getBean(TestBean::class.java)
        assertThat(bean).isSameAs(this.testBean)

        every { testBean.hello() } returns "amock"
        assertThat(bean.hello()).isEqualTo("amock")

        assertThat(TestFactoryBean.USED).isFalse()
    }

    @Order(2)
    @Test
    fun beanReturnedByFactoryIsReset() {
        assertThat(this.testBean.hello()).isEmpty()
    }

    @Configuration(proxyBeanMethods = false)
    class Config {
        @Bean
        fun testFactoryBean(): TestFactoryBean {
            return TestFactoryBean()
        }
    }

    class TestFactoryBean : FactoryBean<TestBean> {
        override fun getObject(): TestBean {
            USED.set(true)
            return TestBean { "normal" }
        }

        override fun getObjectType() = TestBean::class.java

        companion object {
            val USED = AtomicBoolean(false)
        }
    }

    fun interface TestBean {
        fun hello(): String
    }
}
