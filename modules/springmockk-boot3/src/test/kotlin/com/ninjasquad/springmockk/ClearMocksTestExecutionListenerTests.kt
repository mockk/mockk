package com.ninjasquad.springmockk

import com.ninjasquad.springmockk.example.ExampleService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Tests for [ClearMocksTestExecutionListener].
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class ClearMocksTestExecutionListenerTests {

    @Autowired
    private lateinit var context: ApplicationContext

    @RepeatedTest(2)
    fun test(info: RepetitionInfo) {
        if (info.currentRepetition == 1) {
            every { getMock("none").greeting() } returns "none"
            every { getMock("before").greeting() } returns "before"
            every { getMock("after").greeting() } returns "after"
        } else {
            assertThat(getMock("none").greeting()).isEqualTo("none")
            assertThat(getMock("before").greeting()).isNotEqualTo("before")
            assertThat(getMock("after").greeting()).isNotEqualTo("after")
        }
    }

    fun getMock(name: String): ExampleService {
        return this.context.getBean(name, ExampleService::class.java)
    }

    @Configuration
    internal class Config {

        @Bean
        fun before(mockedBeans: MockkCreatedBeans): ExampleService {
            val mock = mockk<ExampleService>(relaxed = true).clear(MockkClear.BEFORE)
            mockedBeans.add(mock)
            return mock
        }

        @Bean
        fun after(mockedBeans: MockkCreatedBeans): ExampleService {
            val mock = mockk<ExampleService>(relaxed = true).clear(MockkClear.AFTER)
            mockedBeans.add(mock)
            return mock
        }

        @Bean
        fun none(mockedBeans: MockkCreatedBeans): ExampleService {
            val mock = mockk<ExampleService>(relaxed = true)
            mockedBeans.add(mock)
            return mock
        }

        @Bean
        @Lazy
        fun fail(): ExampleService {
            // gh-5870
            throw RuntimeException()
        }

        @Bean
        fun brokenFactoryBean(): BrokenFactoryBean {
            // gh-7270
            return BrokenFactoryBean()
        }

    }

    internal class BrokenFactoryBean : FactoryBean<String> {

        override fun getObject(): String? {
            throw IllegalStateException()
        }

        override fun getObjectType(): Class<*>? {
            return String::class.java
        }

        override fun isSingleton(): Boolean {
            return true
        }

    }

}
